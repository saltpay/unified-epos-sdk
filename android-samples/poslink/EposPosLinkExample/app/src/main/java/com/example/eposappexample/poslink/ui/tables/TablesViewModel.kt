package com.example.eposappexample.poslink.ui.tables

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.eposappexample.poslink.models.Product
import com.example.eposappexample.poslink.teya.TeyaUtils
import com.example.eposappexample.poslink.toMinorUnits
import com.teya.unifiedepossdk.poslink.TabEventListener
import com.teya.unifiedepossdk.poslink.models.tabs.ConnectionState
import com.teya.unifiedepossdk.poslink.models.tabs.Tab
import com.teya.unifiedepossdk.poslink.models.tabs.TabBillRequest
import com.teya.unifiedepossdk.poslink.models.tabs.TabCompletion
import com.teya.unifiedepossdk.poslink.models.tabs.TabId
import com.teya.unifiedepossdk.poslink.models.tabs.TabPayRequest
import com.teya.unifiedepossdk.poslink.models.tabs.TabPaymentDetail
import com.teya.unifiedepossdk.poslink.models.tabs.TabStatus
import com.teya.unifiedepossdk.poslink.models.tabs.TabSummary

class TablesViewModel(application: Application) : AndroidViewModel(application) {

    // ---- UI state ----
    var patEnabled by mutableStateOf(false)
        private set
    var tabNameInput by mutableStateOf("")
        private set
    var openTabs by mutableStateOf(listOf<TabSummary>())
        private set
    var selectedTabId by mutableStateOf<TabId?>(null)
        private set
    var selectedTabDetail by mutableStateOf<Tab?>(null)
        private set
    var itemsByTab by mutableStateOf<Map<TabId, List<Product>>>(emptyMap())
        private set
    var showAddTableDialog by mutableStateOf(false)
        private set
    var showProductCatalogue by mutableStateOf(false)
        private set
    var showPaymentsDialog by mutableStateOf(false)
        private set

    val canOpenTab: Boolean get() = tabNameInput.isNotBlank()
    val selectedTabItems: List<Product>
        get() = selectedTabId?.let { itemsByTab[it] } ?: emptyList()
    val selectedTab: TabSummary?
        get() = selectedTabId?.let { id -> openTabs.find { it.tabId == id } }

    private val listener = object : TabEventListener {
        override fun onShowBillRequested(request: TabBillRequest) {
            Log.d(
                TAG,
                "onShowBillRequested(tab=${request.tabId.value}, terminal=${request.terminalId})"
            )
            val items = itemsByTab[request.tabId] ?: emptyList()
            val tab = openTabs.find { it.tabId.value == request.tabId.value } ?: run {
                Log.w(TAG, "Tab not found for bill request: ${request.tabId.value}")
                return
            }

            TeyaUtils.respondToBillRequest(
                tab = tab,
                terminalId = request.terminalId,
                totalAmountMinor = tabTotalMinor(request.tabId),
                billItems = items,
                onSuccess = {
                    Log.d(TAG, "respondToBillRequest success")
                    refreshTab(request.tabId)
                },
                onFailure = { failure -> Log.e(TAG, "respondToBillRequest failure: $failure") }
            )
        }

        override fun onPayRequested(request: TabPayRequest) {
            Log.d(
                TAG,
                "onPayRequested(tab=${request.tabContext.tabId.value}, " +
                        "type=${request.tabContext.type}, amount=${request.amount} ${request.currency})"
            )
            TeyaUtils.makeTabPayment(request.tabContext, request.amount, request.currency)
            refreshTab(request.tabContext.tabId)
        }

        override fun onPaymentProgress(detail: TabPaymentDetail) {
            Log.d(TAG, "onPaymentProgress(tab=${detail.tabId.value}, status=${detail.status})")
            refreshTab(detail.tabId)
        }

        override fun onPaymentCompleted(detail: TabPaymentDetail) {
            Log.d(
                TAG,
                "onPaymentCompleted(tab=${detail.tabId.value}, status=${detail.status}, amount=${detail.amount})"
            )
            refreshTab(detail.tabId)
        }

        override fun onTabPaused(tabId: TabId) {
            Log.d(TAG, "onTabPaused(tab=${tabId.value})")
            refreshTab(tabId)
        }

        override fun onTabResumed(tabId: TabId) {
            Log.d(TAG, "onTabResumed(tab=${tabId.value})")
            refreshTab(tabId)
        }

        override fun onTabCompleted(completion: TabCompletion) {
            Log.d(
                TAG,
                "onTabCompleted(tab=${completion.tabId.value}, totalPaid=${completion.totalPaid})"
            )
            closeTab(completion.tabId)
        }

        override fun onBillHidden(tabId: TabId) {
            Log.d(TAG, "onBillHidden(tab=${tabId.value})")
            refreshTab(tabId)
        }

        override fun onConnectionStateChange(state: ConnectionState) {
            Log.d(TAG, "onConnectionStateChange($state)")
        }

        override fun onUnsubscribed() {
            Log.d(TAG, "onUnsubscribed")
        }
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        patEnabled = prefs.getBoolean(KEY_PAT_ENABLED, false)
        TeyaUtils.subscribeToTabEvents(listener)
        getTabs()
    }

    fun tabTotalMinor(tabId: TabId): Int {
        val items = itemsByTab[tabId] ?: emptyList()
        return items.sumOf { toMinorUnits(it.price * it.quantity) }
    }

    override fun onCleared() {
        TeyaUtils.unsubscribeFromTabEvents(listener)
        super.onCleared()
    }

    fun setPayAtTableEnabled(enable: Boolean) {
        TeyaUtils.setPayAtTableEnabled(
            enable = enable,
            onSuccess = {
                patEnabled = enable
                prefs.edit().putBoolean(KEY_PAT_ENABLED, enable).apply()
                Log.d(TAG, "setPayAtTableEnabled($enable) success")
            },
            onFailure = { failure -> Log.e(TAG, "setPayAtTableEnabled failure: $failure") }
        )
    }

    fun addProduct(product: Product) {
        val tabId = selectedTabId ?: return
        val items = itemsByTab[tabId] ?: emptyList()
        val updated = if (items.any { it.id == product.id }) {
            items.map { if (it.id == product.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            items + product.copy(quantity = 1)
        }
        itemsByTab = itemsByTab + (tabId to updated)
    }

    fun removeProduct(product: Product) {
        val tabId = selectedTabId ?: return
        val items = itemsByTab[tabId] ?: emptyList()
        val existing = items.find { it.id == product.id } ?: return
        val updated = if (existing.quantity > 1) {
            items.map { if (it.id == product.id) it.copy(quantity = it.quantity - 1) else it }
        } else {
            items.filter { it.id != product.id }
        }
        itemsByTab = itemsByTab + (tabId to updated)
    }

    fun updateTabName(value: String) {
        tabNameInput = value
    }

    fun showAddTableDialog() {
        tabNameInput = ""
        showAddTableDialog = true
    }

    fun dismissAddTableDialog() {
        showAddTableDialog = false
        tabNameInput = ""
    }

    fun openTableDetails(tabId: TabId) {
        selectedTabId = tabId
        selectedTabDetail = null
        refreshSelectedTabDetail()
    }

    fun closeTableDetails() {
        selectedTabId = null
        selectedTabDetail = null
        showProductCatalogue = false
        showPaymentsDialog = false
    }

    fun refreshSelectedTabDetail() {
        selectedTabId?.let { refreshTab(it) }
    }

    fun showProductCatalogue() {
        showProductCatalogue = true
    }

    fun dismissProductCatalogue() {
        showProductCatalogue = false
    }

    fun showPaymentsDialog() {
        showPaymentsDialog = true
    }

    fun dismissPaymentsDialog() {
        showPaymentsDialog = false
    }

    fun getTabs() {
        TeyaUtils.listTabs(
            onSuccess = { page ->
                openTabs = page.items
                Log.d(TAG, "listTabs -> ${page.items.size} tab(s)")
            },
            onFailure = { failure -> Log.e(TAG, "listTabs failure: $failure") }
        )
    }

    fun openTab() {
        if (!canOpenTab) return
        val tabId = "tab-${System.currentTimeMillis()}"
        TeyaUtils.openTab(
            tabId = tabId,
            tabName = tabNameInput,
            onSuccess = { tab ->
                itemsByTab = itemsByTab + (tab.tabId to emptyList())
                openTabs = upsertTab(tab.toSummary())
                tabNameInput = ""
                showAddTableDialog = false
                Log.d(TAG, "openTab(${tab.tabId.value}, '${tab.tabName}') success")
            },
            onFailure = { failure -> Log.e(TAG, "openTab failure: $failure") }
        )
    }

    fun closeTab(tabId: TabId) {
        TeyaUtils.closeTab(
            tabId = tabId,
            onSuccess = {
                openTabs = removeTab(tabId)
                itemsByTab = itemsByTab - tabId
                if (selectedTabId == tabId) selectedTabId = null
                Log.d(TAG, "closeTab(${tabId.value}) success")
            },
            onFailure = { failure -> Log.e(TAG, "closeTab failure: $failure") }
        )
    }

    private fun refreshTab(tabId: TabId) {
        TeyaUtils.getTab(
            tabId = tabId,
            onSuccess = { tab ->
                openTabs = upsertTab(tab.toSummary())
                if (tabId == selectedTabId) selectedTabDetail = tab
                Log.d(
                    TAG,
                    "getTab(${tab.tabId.value}) -> status=${tab.status}, paid=${tab.totalPaid}, payments=${tab.paymentRequests?.size ?: 0}"
                )
            },
            onFailure = { failure -> Log.e(TAG, "getTab failure: $failure") }
        )
    }

    private fun upsertTab(tab: TabSummary): List<TabSummary> {
        if (tab.status == TabStatus.CLOSED) return removeTab(tab.tabId)
        val index = openTabs.indexOfFirst { it.tabId == tab.tabId }
        return if (index == -1) openTabs + tab
        else openTabs.toMutableList().apply { this[index] = tab }
    }

    private fun removeTab(tabId: TabId): List<TabSummary> {
        return openTabs.filterNot { it.tabId == tabId }
    }

    private companion object {
        const val TAG = "TablesViewModel"
        const val PREFS_NAME = "pat_prefs"
        const val KEY_PAT_ENABLED = "pat_enabled"
    }
}

private fun Tab.toSummary() = TabSummary(
    tabId = tabId,
    tabName = tabName,
    status = status,
    totalAmount = totalAmount,
    totalPaid = totalPaid,
    remaining = remaining,
    currency = currency,
    showingBillTerminalId = showingBillTerminalId
)