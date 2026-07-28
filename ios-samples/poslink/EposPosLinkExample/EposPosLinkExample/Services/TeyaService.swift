import TeyaUnifiedEposSDK

final class TeyaService {
    static let shared = TeyaService()
    
    let teyaPosLinkSDK = TeyaPosLinkSDKKt.initialize(
        authConfig: PosLinkSDKAuthConfigManaged(
            clientId: "",              // Replace with your Client ID
            clientSecret: ""           // Replace with your Client Secret
        ),
        isProductionEnv: false,  // Set to true for production
        eposInstanceId: nil,     // Optional: identifier for your ePOS app instance
        logger: SDKLoggerImpl()  // Optional: your custom logger implementation
    )
    
    private class SDKLoggerImpl: TeyaSDKLogger {
        func d(message: String) {
            print("[DEBUG] SDK: \(message)")
        }
        
        func i(message: String) {
            print("[INFO] SDK: \(message)")
        }
        
        func w(message: String) {
            print("[WARNING] SDK: \(message)")
        }
        
        func e(message: String) {
            print("[ERROR] SDK: \(message)")
        }
    }
    
    func setUp() {
        teyaPosLinkSDK.setup(
            onFailure: { failure in
                print("Failed to initialize TeyaPosLinkSDK: \(failure)")
            },
            onSuccess: {
                print("TeyaPosLinkSDK initialized successfully")
            }
        )
    }
    
    func clearUserAuth() {
        teyaPosLinkSDK.clearUserAuth()
    }
    
    func clearDeviceLink() {
        teyaPosLinkSDK.clearDeviceLink()
    }
    
    func makePayment(totalMinorUnits: Int32, tipMinorUnits: Int32) {
        let paymentSubscription = teyaPosLinkSDK.transactionsApi.makePayment(
            transactionId: UUID().uuidString, // or pass whatever identifier you already have for the payment you're about to make
            amount: totalMinorUnits, // the total amount to be paid including the tip, in the smallest unit of the currency (e.g., cents).
            currency: PriceUtils.currencyCode, // The ISO 4217 currency code (e.g., "GBP", "EUR").
            tip: tipMinorUnits.toKotlinInt(), // An optional tip amount, in the smallest unit of the currency.
            purchaseData: nil
        )
        
        paymentSubscription.subscribe(listener: PaymentStateChangeListener(type: .payment))
        paymentSubscription.subscribe(
            listener: TeyaPosLinkInProgressUiKt.create(
                autoDismissOnFinalStateAfterMs: 2000, // Time in ms before the UI auto-dismisses after a final state
                onDismiss: { state in // Optional callback invoked after dismissing the UI with the current PaymentStateDetails.
                    print("Payment UI dismissed with payment state details: \(state)")
                }
            )
        )
    }

    // ---- Refund ----

    func refundPayment(
        gatewayPaymentId: String,
        amountMinor: Int32,
        currency: String,
        onSettled: @escaping () -> Void
    ) {
        let refundSubscription = teyaPosLinkSDK.transactionsApi.refundPayment(
            paymentId: TeyaGatewayPaymentId(id: gatewayPaymentId),
            amount: amountMinor,
            currency: currency,
            allocatedLineItems: nil
        )
        refundSubscription.subscribe(refundListener: RefundResultListener { refundResult in
            print("Refund result: \(refundResult)")
            TransactionRecorder.recordRefund(
                refundResult: refundResult,
                gatewayPaymentId: gatewayPaymentId,
                amountMinor: amountMinor,
                currency: currency
            )
            onSettled()
        })
    }

    func makeUnreferencedRefund(amountMinorUnits: Int32) {
        let refundSubscription = teyaPosLinkSDK.transactionsApi.makeUnreferencedRefund(
            transactionId: UUID().uuidString,
            amount: amountMinorUnits,
            currency: PriceUtils.currencyCode
        )

        refundSubscription.subscribe(listener: PaymentStateChangeListener(type: .refund))
        refundSubscription.subscribe(
            listener: TeyaPosLinkInProgressUiKt.create(
                autoDismissOnFinalStateAfterMs: 2000,
                onDismiss: { state in
                    print("Unreferenced refund UI dismissed with payment state details: \(state)")
                }
            )
        )
    }
    
    func printReceipt(products: [Product], tip: Double) {
        let template = PrintUtils.buildCustomPrintTemplate(products: products, tip: tip)
        teyaPosLinkSDK.printingApi.printCustomTemplate(template: template).subscribe(
            printingListener: PrintingStatusSubscriptionListener()
        )
    }
    
    // ---- Pay at Table ----
    
    /// Enables or disables Pay at Table for the linked store.
    func setPayAtTableEnabled(
        _ enable: Bool,
        onSuccess: @escaping () -> Void,
        onFailure: @escaping () -> Void
    ) {
        teyaPosLinkSDK.tabsApi.setPayAtTableEnabledOnStore(
            enable: enable,
            onSuccess: onSuccess,
            onFailure: { failure in
                print("setPayAtTableEnabled failure: \(failure.reason)")
                onFailure()
            }
        )
    }
    
    func openTab(
        tabId: String,
        tabName: String,
        onSuccess: @escaping (TeyaTab) -> Void,
        onFailure: @escaping () -> Void
    ) {
        teyaPosLinkSDK.tabsApi.openTab(
            tabId: TeyaTabId(value: tabId),
            tabName: tabName,
            currency: PriceUtils.currencyCode,
            onSuccess: onSuccess,
            onFailure: { failure in
                print("openTab failure: \(failure.reason)")
                onFailure()
            }
        )
    }
    
    func listTabs(
        onSuccess: @escaping (TeyaTabPage) -> Void,
        onFailure: @escaping () -> Void
    ) {
        teyaPosLinkSDK.tabsApi.listTabs(
            statuses: nil,
            after: nil,
            before: nil,
            limit: nil,
            onSuccess: onSuccess,
            onFailure: { failure in
                print("listTabs failure: \(failure.reason)")
                onFailure()
            }
        )
    }
    
    func getTab(
        _ tabId: TeyaTabId,
        onSuccess: @escaping (TeyaTab) -> Void,
        onFailure: @escaping () -> Void
    ) {
        teyaPosLinkSDK.tabsApi.getTab(
            tabId: tabId,
            onSuccess: onSuccess,
            onFailure: { failure in
                print("getTab failure: \(failure.reason)")
                onFailure()
            }
        )
    }
    
    func closeTab(
        _ tabId: TeyaTabId,
        onSuccess: @escaping () -> Void,
        onFailure: @escaping () -> Void
    ) {
        teyaPosLinkSDK.tabsApi.closeTab(
            tabId: tabId,
            onSuccess: onSuccess,
            onFailure: { failure in
                print("closeTab failure: \(failure.reason)")
                onFailure()
            }
        )
    }
    
    /// Responds to a SHOW_BILL_REQUEST by sending the bill back to the requesting terminal.
    func respondToBillRequest(
        tab: TeyaTabSummary,
        terminalId: String,
        totalAmountMinor: Int32,
        billItems: [Product],
        onSuccess: @escaping () -> Void,
        onFailure: @escaping () -> Void
    ) {
        teyaPosLinkSDK.tabsApi.respondToBillRequest(
            tabId: tab.tabId,
            terminalId: terminalId,
            totalAmount: totalAmountMinor,
            currency: PriceUtils.currencyCode,
            printModel: PrintUtils.buildTableBillTemplate(tab: tab, items: billItems, totalMinor: Int(totalAmountMinor)),
            billImage: nil,
            onSuccess: onSuccess,
            onFailure: { failure in
                print("respondToBillRequest failure: \(failure.reason)")
                onFailure()
            }
        )
    }
    
    /// Responds to a PAY_REQUEST by starting a tab-tagged payment and logging its state.
    func makeTabPayment(tabContext: TeyaTabPaymentContext, amount: Int32, currency: String) {
        let subscription = teyaPosLinkSDK.transactionsApi.makePayment(
            transactionId: UUID().uuidString,
            amount: amount,
            currency: currency,
            tip: nil,
            purchaseData: nil,
            tabContext: tabContext
        )
        subscription.subscribe(
            listener: PaymentStateChangeListener(
                type: .payment,
                isTab: true,
                isCash: tabContext.method == TeyaTabPaymentMethod.cash
            )
        )
    }

    func subscribeToTabEvents(_ listener: TeyaTabEventListener) {
        teyaPosLinkSDK.tabsApi.tabEvents.subscribe(tabEventListener: listener)
    }
    
    func unsubscribeFromTabEvents(_ listener: TeyaTabEventListener) {
        teyaPosLinkSDK.tabsApi.tabEvents.unsubscribe(tabEventListener: listener)
    }
    
    private class PaymentStateChangeListener: TeyaPaymentStateChangeListener {
        private let type: TeyaTransactionType
        private let isTab: Bool
        private let isCash: Bool

        init(type: TeyaTransactionType, isTab: Bool = false, isCash: Bool = false) {
            self.type = type
            self.isTab = isTab
            self.isCash = isCash
        }

        func onPaymentStateChanged(state: TeyaPaymentStateDetails) {
            print("Payment state changed: \(state), is it a final state = \(state.isFinal)")
            TransactionRecorder.recordPaymentIfFinal(state: state, type: type, isTab: isTab, isCash: isCash)
        }
    }

    private class RefundResultListener: TeyaRefundResultListener {
        private let onResult: (TeyaRefundResultDetails) -> Void

        init(_ onResult: @escaping (TeyaRefundResultDetails) -> Void) {
            self.onResult = onResult
        }

        func onRefundResult(refundResult: TeyaRefundResultDetails) {
            onResult(refundResult)
        }
    }

    private final class PrintingStatusSubscriptionListener: TeyaPrintingStatusSubscriptionListener {
        func onPrintingStateChanged(printStateDetails: TeyaPrintStateDetails) {
            print("Printing state changed: \(printStateDetails)")
        }
    }
}

extension Int32 {
    func toKotlinInt() -> KotlinInt {
        return KotlinInt(int: self)
    }
}

extension KotlinInt {
    var asInt: Int { Int(truncating: self) }
}

extension KotlinLong {
    var asInt64: Int64 { Int64(truncating: self) }
}
