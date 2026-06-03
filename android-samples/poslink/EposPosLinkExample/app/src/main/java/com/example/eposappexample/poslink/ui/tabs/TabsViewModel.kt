package com.example.eposappexample.poslink.ui.tabs

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.eposappexample.poslink.TeyaUtils
import com.example.eposappexample.poslink.models.Product
import com.example.eposappexample.poslink.toMinorUnits
import com.teya.unifiedepossdk.poslink.TabEventListener
import com.teya.unifiedepossdk.poslink.models.tabs.ConnectionState
import com.teya.unifiedepossdk.poslink.models.tabs.TabBillRequest
import com.teya.unifiedepossdk.poslink.models.tabs.TabCompletion
import com.teya.unifiedepossdk.poslink.models.tabs.TabId
import com.teya.unifiedepossdk.poslink.models.tabs.TabPayRequest
import com.teya.unifiedepossdk.poslink.models.tabs.TabPaymentDetail
import com.teya.unifiedepossdk.poslink.models.tabs.TabSummary

/** Pure list/log operations used by [TabsViewModel]. */
object TabsLogic {
    const val MAX_LOG_ENTRIES = 30

    fun prependLog(current: List<String>, message: String, limit: Int = MAX_LOG_ENTRIES): List<String> =
        (listOf(message) + current).take(limit)

    fun upsertTab(current: List<TabSummary>, tab: TabSummary): List<TabSummary> {
        val index = current.indexOfFirst { it.tabId == tab.tabId }
        return if (index == -1) current + tab
        else current.toMutableList().apply { this[index] = tab }
    }

    fun removeTab(current: List<TabSummary>, tabId: TabId): List<TabSummary> =
        current.filterNot { it.tabId == tabId }

    fun addProduct(items: List<Product>, product: Product): List<Product> {
        val existing = items.find { it.id == product.id }
        return if (existing != null) {
            items.map { if (it.id == product.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            items + product.copy(quantity = 1)
        }
    }

    fun removeProduct(items: List<Product>, product: Product): List<Product> {
        val existing = items.find { it.id == product.id } ?: return items
        return if (existing.quantity > 1) {
            items.map { if (it.id == product.id) it.copy(quantity = it.quantity - 1) else it }
        } else {
            items.filter { it.id != product.id }
        }
    }
}

class TabsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- UI state ----
    var patEnabled by mutableStateOf(prefs.getBoolean(KEY_PAT_ENABLED, false))
        private set
    var tabNameInput by mutableStateOf("")
        private set
    var openTabs by mutableStateOf(listOf<TabSummary>())
        private set
    var eventLog by mutableStateOf(listOf<String>())
        private set
    var showAddTableDialog by mutableStateOf(false)
        private set
    var showProductCatalogue by mutableStateOf(false)
        private set

    var selectedTabId by mutableStateOf<TabId?>(null)
        private set

    var itemsByTab by mutableStateOf<Map<String, List<Product>>>(emptyMap())
        private set

    val canOpenTab: Boolean get() = tabNameInput.isNotBlank()

    val selectedTabItems: List<Product>
        get() = selectedTabId?.let { itemsByTab[it.value] } ?: emptyList()

    val selectedTab: TabSummary?
        get() = selectedTabId?.let { id -> openTabs.find { it.tabId == id } }

    fun tabTotalMinor(tabId: TabId): Int = totalMinor(itemsByTab[tabId.value] ?: emptyList())

    private fun totalMinor(items: List<Product>): Int =
        items.sumOf { toMinorUnits(it.price * it.quantity) }

    private val listener = object : TabEventListener {
        override fun onShowBillRequested(request: TabBillRequest) {
            log("onShowBillRequested(tab=${request.tabId.value}, terminal=${request.terminalId})")
            val items = itemsByTab[request.tabId.value] ?: emptyList()
            TeyaUtils.respondToBillRequest(
                tabId = request.tabId,
                terminalId = request.terminalId,
                totalAmountMinor = totalMinor(items),
                billItems = items,
                onSuccess = { log("respondToBillRequest success") },
                onFailure = { failure -> log("respondToBillRequest failure: $failure") }
            )
        }

        override fun onPayRequested(request: TabPayRequest) {
            log(
                "onPayRequested(tab=${request.tabContext.tabId.value}, " +
                    "type=${request.tabContext.type}, amount=${request.amount} ${request.currency})"
            )
            TeyaUtils.makeTabPayment(request.tabContext, request.amount, request.currency)
        }

        override fun onPaymentProgress(detail: TabPaymentDetail) {
            log("onPaymentProgress(tab=${detail.tabId.value}, status=${detail.status})")
        }

        override fun onPaymentCompleted(detail: TabPaymentDetail) {
            log("onPaymentCompleted(tab=${detail.tabId.value}, status=${detail.status}, amount=${detail.amount})")
        }

        override fun onTabPaused(tabId: TabId) { log("onTabPaused(tab=${tabId.value})") }

        override fun onTabResumed(tabId: TabId) { log("onTabResumed(tab=${tabId.value})") }

        override fun onTabCompleted(completion: TabCompletion) {
            log("onTabCompleted(tab=${completion.tabId.value}, totalPaid=${completion.totalPaid})")
            closeTab(completion.tabId)
        }

        override fun onBillHidden(tabId: TabId) { log("onBillHidden(tab=${tabId.value})") }

        override fun onConnectionStateChange(state: ConnectionState) {
            log("onConnectionStateChange($state)")
        }

        override fun onUnsubscribed() {
            log("onUnsubscribed")
        }
    }

    init {
        TeyaUtils.subscribeToTabEvents(listener)
        refreshTabs()
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
                log("setPayAtTableEnabled($enable) success")
            },
            onFailure = { failure -> log("setPayAtTableEnabled failure: $failure") }
        )
    }

    fun addProduct(product: Product) {
        val tabKey = selectedTabId?.value ?: return
        val updated = TabsLogic.addProduct(itemsByTab[tabKey] ?: emptyList(), product)
        itemsByTab = itemsByTab + (tabKey to updated)
    }

    fun removeProduct(product: Product) {
        val tabKey = selectedTabId?.value ?: return
        val updated = TabsLogic.removeProduct(itemsByTab[tabKey] ?: emptyList(), product)
        itemsByTab = itemsByTab + (tabKey to updated)
    }

    fun updateTabName(value: String) { tabNameInput = value }

    fun showAddTableDialog() {
        tabNameInput = ""
        showAddTableDialog = true
    }

    fun dismissAddTableDialog() {
        showAddTableDialog = false
        tabNameInput = ""
    }

    fun openTableDetails(tabId: TabId) { selectedTabId = tabId }

    fun closeTableDetails() {
        selectedTabId = null
        showProductCatalogue = false
    }

    fun showProductCatalogue() { showProductCatalogue = true }

    fun dismissProductCatalogue() { showProductCatalogue = false }

    fun openTab() {
        if (!canOpenTab) return
        val tabId = "tab-${System.currentTimeMillis()}"
        TeyaUtils.openTab(
            tabId = tabId,
            tabName = tabNameInput,
            onSuccess = { tab ->
                itemsByTab = itemsByTab + (tab.tabId.value to emptyList())
                openTabs = TabsLogic.upsertTab(openTabs, tab.toSummary())
                tabNameInput = ""
                showAddTableDialog = false
                log("openTab(${tab.tabId.value}, '${tab.tabName}') success")
            },
            onFailure = { failure -> log("openTab failure: $failure") }
        )
    }

    fun refreshTabs() {
        TeyaUtils.listTabs(
            onSuccess = { page ->
                openTabs = page.items
                log("listTabs -> ${page.items.size} tab(s)")
            },
            onFailure = { failure -> log("listTabs failure: $failure") }
        )
    }

    fun closeTab(tabId: TabId) {
        TeyaUtils.closeTab(
            tabId = tabId,
            onSuccess = {
                openTabs = TabsLogic.removeTab(openTabs, tabId)
                itemsByTab = itemsByTab - tabId.value
                if (selectedTabId == tabId) selectedTabId = null
                log("closeTab(${tabId.value}) success")
            },
            onFailure = { failure -> log("closeTab failure: $failure") }
        )
    }

    private fun log(message: String) {
        eventLog = TabsLogic.prependLog(eventLog, message)
    }

    private companion object {
        const val PREFS_NAME = "pat_prefs"
        const val KEY_PAT_ENABLED = "pat_enabled"
    }
}

private fun com.teya.unifiedepossdk.poslink.models.tabs.Tab.toSummary() = TabSummary(
    tabId = tabId,
    tabName = tabName,
    status = status,
    totalAmount = totalAmount,
    totalPaid = totalPaid,
    remaining = remaining,
    currency = currency,
    showingBillTerminalId = showingBillTerminalId
)