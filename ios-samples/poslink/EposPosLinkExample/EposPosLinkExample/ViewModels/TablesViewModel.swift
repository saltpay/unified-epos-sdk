import Foundation
import TeyaUnifiedEposSDK

@Observable
final class TablesViewModel {

    // ---- UI state ----
    private(set) var patEnabled: Bool = UserDefaults.standard.bool(forKey: Keys.patEnabled)
    private(set) var tabNameInput: String = ""
    private(set) var openTabs: [TeyaTabSummary] = []
    private(set) var selectedTabId: TeyaTabId?
    private(set) var selectedTabDetail: TeyaTab?
    private(set) var itemsByTab: [String: [Product]] = [:]
    var showAddTableDialog: Bool = false
    var showProductCatalogue: Bool = false
    var showPaymentsDialog: Bool = false

    var canOpenTab: Bool { !tabNameInput.trimmingCharacters(in: .whitespaces).isEmpty }

    var selectedTabItems: [Product] {
        guard let id = selectedTabId?.value else { return [] }
        return itemsByTab[id] ?? []
    }

    var selectedTab: TeyaTabSummary? {
        guard let id = selectedTabId?.value else { return nil }
        return openTabs.first { $0.tabId.value == id }
    }

    @ObservationIgnored private var listener: TabEventListenerImpl!
    @ObservationIgnored private var closingTabs: Set<String> = []

    init() {
        listener = TabEventListenerImpl(viewModel: self)
        TeyaService.shared.subscribeToTabEvents(listener)
        getTabs()
    }

    deinit {
        TeyaService.shared.unsubscribeFromTabEvents(listener)
    }

    func tabTotalMinor(_ tabIdValue: String) -> Int {
        let items = itemsByTab[tabIdValue] ?? []
        return items.reduce(0) { $0 + PriceUtils.toMinorUnits($1.price * Double($1.quantity)) }
    }

    func setPayAtTableEnabled(_ enable: Bool) {
        TeyaService.shared.setPayAtTableEnabled(
            enable,
            onSuccess: { [weak self] in
                self?.patEnabled = enable
                UserDefaults.standard.set(enable, forKey: Keys.patEnabled)
                print("setPayAtTableEnabled(\(enable)) success")
            },
            onFailure: { print("setPayAtTableEnabled failed") }
        )
    }

    func addProduct(_ product: Product) {
        guard let id = selectedTabId?.value else { return }
        var items = itemsByTab[id] ?? []
        if let index = items.firstIndex(where: { $0.id == product.id }) {
            items[index].quantity += 1
        } else {
            var newProduct = product
            newProduct.quantity = 1
            items.append(newProduct)
        }
        itemsByTab[id] = items
    }

    func removeProduct(_ product: Product) {
        guard let id = selectedTabId?.value else { return }
        var items = itemsByTab[id] ?? []
        guard let index = items.firstIndex(where: { $0.id == product.id }) else { return }
        if items[index].quantity > 1 {
            items[index].quantity -= 1
        } else {
            items.remove(at: index)
        }
        itemsByTab[id] = items
    }

    func updateTabName(_ value: String) { tabNameInput = value }

    func showAddTable() {
        tabNameInput = ""
        showAddTableDialog = true
    }

    func dismissAddTable() {
        showAddTableDialog = false
        tabNameInput = ""
    }

    func openTableDetails(_ tabId: TeyaTabId) {
        selectedTabId = tabId
        selectedTabDetail = nil
        refreshSelectedTabDetail()
    }

    func closeTableDetails() {
        selectedTabId = nil
        selectedTabDetail = nil
        showProductCatalogue = false
        showPaymentsDialog = false
    }

    func refreshSelectedTabDetail() {
        if let id = selectedTabId { refreshTab(id) }
    }

    func getTabs() {
        TeyaService.shared.listTabs(
            onSuccess: { [weak self] page in
                self?.openTabs = page.items
                print("listTabs -> \(page.items.count) tab(s)")
            },
            onFailure: { print("listTabs failed") }
        )
    }

    func openTab() {
        guard canOpenTab else { return }
        let tabId = "tab-\(Int(Date().timeIntervalSince1970 * 1000))"
        TeyaService.shared.openTab(
            tabId: tabId,
            tabName: tabNameInput,
            onSuccess: { [weak self] tab in
                guard let self else { return }
                self.itemsByTab[tab.tabId.value] = []
                self.openTabs = self.upsert(tab.toSummary())
                self.tabNameInput = ""
                self.showAddTableDialog = false
                print("openTab(\(tab.tabId.value), '\(tab.tabName)') success")
            },
            onFailure: { print("openTab failed") }
        )
    }

    func closeTab(_ tabId: TeyaTabId) {
        TeyaService.shared.closeTab(
            tabId,
            onSuccess: { [weak self] in
                guard let self else { return }
                self.closingTabs.remove(tabId.value)
                self.openTabs = self.remove(tabId)
                self.itemsByTab[tabId.value] = nil
                if self.selectedTabId?.value == tabId.value { self.selectedTabId = nil }
                print("closeTab(\(tabId.value)) success")
            },
            onFailure: { [weak self] in
                self?.closingTabs.remove(tabId.value)
                print("closeTab failed")
            }
        )
    }

    private func refreshTab(_ tabId: TeyaTabId) {
        TeyaService.shared.getTab(
            tabId,
            onSuccess: { [weak self] tab in
                guard let self else { return }
                self.openTabs = self.upsert(tab.toSummary())
                if tabId.value == self.selectedTabId?.value { self.selectedTabDetail = tab }
                print("getTab(\(tab.tabId.value)) -> status=\(tab.status), paid=\(tab.totalPaid?.asInt ?? 0)")
                if tab.status == TeyaTabStatus.completed {
                    self.autoCloseCompletedTab(tab.tabId)
                }
            },
            onFailure: { print("getTab failed") }
        )
    }

    private func autoCloseCompletedTab(_ tabId: TeyaTabId) {
        guard !closingTabs.contains(tabId.value) else { return }
        closingTabs.insert(tabId.value)
        print("Tab \(tabId.value) fully paid (COMPLETED) -> closing")
        closeTab(tabId)
    }

    private func upsert(_ tab: TeyaTabSummary) -> [TeyaTabSummary] {
        if tab.status == TeyaTabStatus.closed { return remove(tab.tabId) }
        var tabs = openTabs
        if let index = tabs.firstIndex(where: { $0.tabId.value == tab.tabId.value }) {
            tabs[index] = tab
        } else {
            tabs.append(tab)
        }
        return tabs
    }

    private func remove(_ tabId: TeyaTabId) -> [TeyaTabSummary] {
        openTabs.filter { $0.tabId.value != tabId.value }
    }

    func handleShowBillRequested(_ request: TeyaTabBillRequest) {
        if let tab = openTabs.first(where: { $0.tabId.value == request.tabId.value }) {
            respondToBill(tab: tab, terminalId: request.terminalId)
        } else {
            print("Tab not cached for bill request, fetching: \(request.tabId.value)")
            TeyaService.shared.getTab(
                request.tabId,
                onSuccess: { [weak self] tab in
                    guard let self else { return }
                    self.openTabs = self.upsert(tab.toSummary())
                    self.respondToBill(tab: tab.toSummary(), terminalId: request.terminalId)
                },
                onFailure: { print("getTab for bill request failed") }
            )
        }
    }

    private func respondToBill(tab: TeyaTabSummary, terminalId: String) {
        TeyaService.shared.respondToBillRequest(
            tab: tab,
            terminalId: terminalId,
            totalAmountMinor: Int32(tabTotalMinor(tab.tabId.value)),
            billItems: itemsByTab[tab.tabId.value] ?? [],
            onSuccess: { [weak self] in
                print("respondToBillRequest success")
                self?.refreshTab(tab.tabId)
            },
            onFailure: { print("respondToBillRequest failed") }
        )
    }

    func handlePayRequested(_ request: TeyaTabPayRequest) {
        TeyaService.shared.makeTabPayment(
            tabContext: request.tabContext,
            amount: request.amount,
            currency: request.currency
        )
        refreshTab(request.tabContext.tabId)
    }

    func handlePaymentUpdate(_ tabId: TeyaTabId) {
        refreshTab(tabId)
    }

    func handleTabCompleted(_ tabId: TeyaTabId) {
        refreshTab(tabId)
    }

    private enum Keys {
        static let patEnabled = "pat_enabled"
    }
}

private extension TeyaTab {
    func toSummary() -> TeyaTabSummary {
        TeyaTabSummary(
            tabId: tabId,
            tabName: tabName,
            status: status,
            totalAmount: totalAmount,
            totalPaid: totalPaid,
            remaining: remaining,
            currency: currency,
            showingBillTerminalId: showingBillTerminalId
        )
    }
}

final class TabEventListenerImpl: TeyaTabEventListener {
    private weak var viewModel: TablesViewModel?

    init(viewModel: TablesViewModel) {
        self.viewModel = viewModel
    }

    func onShowBillRequested(request: TeyaTabBillRequest) {
        print("onShowBillRequested(tab=\(request.tabId.value), terminal=\(request.terminalId))")
        DispatchQueue.main.async { self.viewModel?.handleShowBillRequested(request) }
    }

    func onPayRequested(request: TeyaTabPayRequest) {
        print("onPayRequested(tab=\(request.tabContext.tabId.value), type=\(request.tabContext.type), amount=\(request.amount) \(request.currency))")
        DispatchQueue.main.async { self.viewModel?.handlePayRequested(request) }
    }

    func onPaymentProgress(detail: TeyaTabPaymentDetail) {
        print("onPaymentProgress(tab=\(detail.tabId.value), status=\(detail.status))")
        DispatchQueue.main.async { self.viewModel?.handlePaymentUpdate(detail.tabId) }
    }

    func onPaymentCompleted(detail: TeyaTabPaymentDetail) {
        print("onPaymentCompleted(tab=\(detail.tabId.value), status=\(detail.status), amount=\(detail.amount))")
        DispatchQueue.main.async { self.viewModel?.handlePaymentUpdate(detail.tabId) }
    }

    func onTabPaused(tabId: TeyaTabId) {
        print("onTabPaused(tab=\(tabId.value))")
        DispatchQueue.main.async { self.viewModel?.handlePaymentUpdate(tabId) }
    }

    func onTabResumed(tabId: TeyaTabId) {
        print("onTabResumed(tab=\(tabId.value))")
        DispatchQueue.main.async { self.viewModel?.handlePaymentUpdate(tabId) }
    }

    func onTabCompleted(completion: TeyaTabCompletion) {
        print("onTabCompleted(tab=\(completion.tabId.value), totalPaid=\(completion.totalPaid))")
        DispatchQueue.main.async { self.viewModel?.handleTabCompleted(completion.tabId) }
    }

    func onBillHidden(tabId: TeyaTabId) {
        print("onBillHidden(tab=\(tabId.value))")
        DispatchQueue.main.async { self.viewModel?.handlePaymentUpdate(tabId) }
    }

    func onConnectionStateChange(state: TeyaTabConnectionState) {
        print("onConnectionStateChange(\(state))")
    }

    func onUnsubscribed() {
        print("onUnsubscribed")
    }
}
