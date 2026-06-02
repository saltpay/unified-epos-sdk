package com.example.eposappexample.poslink.ui.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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
}

class TabsViewModel : ViewModel() {

    // ---- UI state ----
    var patEnabled by mutableStateOf(false)
        private set
    var connectionState by mutableStateOf(ConnectionState.Disconnected)
        private set
    var basket by mutableStateOf(listOf<Product>())
        private set
    var tabNameInput by mutableStateOf("")
        private set
    var openTabs by mutableStateOf(listOf<TabSummary>())
        private set
    var eventLog by mutableStateOf(listOf<String>())
        private set

    val basketItemCount: Int get() = basket.sumOf { it.quantity }
    val basketTotalMinor: Int get() = basket.sumOf { toMinorUnits(it.price * it.quantity) }
    val canOpenTab: Boolean get() = basket.isNotEmpty() && tabNameInput.isNotBlank()

    // Remember the basket used to open each tab so we can render its bill on request.
    private val basketByTab = mutableMapOf<String, List<Product>>()

    private val listener = object : TabEventListener {
        override fun onShowBillRequested(request: TabBillRequest) {
            log("onShowBillRequested(tab=${request.tabId.value}, terminal=${request.terminalId})")
            val items = basketByTab[request.tabId.value] ?: emptyList()
            val total = items.sumOf { toMinorUnits(it.price * it.quantity) }
            TeyaUtils.respondToBillRequest(
                tabId = request.tabId,
                terminalId = request.terminalId,
                totalAmountMinor = total,
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
            connectionState = state
            log("onConnectionStateChange($state)")
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
                log("setPayAtTableEnabled($enable) success")
            },
            onFailure = { failure -> log("setPayAtTableEnabled failure: $failure") }
        )
    }

    fun addProduct(product: Product) {
        val existing = basket.find { it.id == product.id }
        basket = if (existing != null) {
            basket.map { if (it.id == product.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            basket + product.copy(quantity = 1)
        }
    }

    fun removeProduct(product: Product) {
        val existing = basket.find { it.id == product.id } ?: return
        basket = if (existing.quantity > 1) {
            basket.map { if (it.id == product.id) it.copy(quantity = it.quantity - 1) else it }
        } else {
            basket.filter { it.id != product.id }
        }
    }

    fun updateTabName(value: String) { tabNameInput = value }

    fun openTab() {
        if (!canOpenTab) return
        val tabId = "tab-${System.currentTimeMillis()}"
        val itemsForTab = basket
        TeyaUtils.openTab(
            tabId = tabId,
            tabName = tabNameInput,
            onSuccess = { tab ->
                basketByTab[tab.tabId.value] = itemsForTab
                openTabs = TabsLogic.upsertTab(openTabs, tab.toSummary())
                basket = emptyList()
                tabNameInput = ""
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
                basketByTab.remove(tabId.value)
                log("closeTab(${tabId.value}) success")
            },
            onFailure = { failure -> log("closeTab failure: $failure") }
        )
    }

    private fun log(message: String) {
        eventLog = TabsLogic.prependLog(eventLog, message)
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
