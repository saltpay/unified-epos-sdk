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
        
        paymentSubscription.subscribe(listener: PaymentStateChangeListener())
        paymentSubscription.subscribe(
            listener: TeyaPosLinkInProgressUiKt.create(
                autoDismissOnFinalStateAfterMs: 2000, // Time in ms before the UI auto-dismisses after a final state
                onDismiss: { state in // Optional callback invoked after dismissing the UI with the current PaymentStateDetails.
                    print("Payment UI dismissed with payment state details: \(state)")
                }
            )
        )
    }
    
    func printReceipt(products: [Product], tip: Double) {
        let template = buildCustomPrintTemplate(products: products, tip: tip)
        teyaPosLinkSDK.printingApi.printCustomTemplate(template: template).subscribe(
            printingListener: PrintingStatusSubscriptionListener()
        )
    }
    
    private func buildCustomPrintTemplate(products: [Product], tip: Double) -> TeyaTemplate {
        let df = DateFormatter()
        df.dateFormat = "dd/MM/yy · HH:mm"
        let nowText = df.string(from: Date())
        
        let subtotal = products.reduce(0.0) { $0 + $1.price * Double($1.quantity) }
        let total = subtotal + tip
        
        var rows: [TeyaReceiptRow] = []

        rows.append(
            TeyaReceiptRowItems(
                items: [
                    TeyaRowElementText(text: "CUSTOMER RECEIPT", bold: true, align: TeyaAlign.left),
                    TeyaRowElementText(text: nowText, bold: true, align: TeyaAlign.right),
                ]
            )
        )
        
        rows.append(TeyaReceiptRowSpacer.shared)
        rows.append(TeyaReceiptRowDivider.shared)

        for product in products {
            rows.append(
                TeyaReceiptRowItems(
                    items: [
                        TeyaRowElementText(
                            text: "\(product.quantity)x \(product.name.uppercased())",
                            bold: true,
                            align: TeyaAlign.left
                        ),
                        TeyaRowElementText(
                            text: PriceUtils.formatPrice(product.price * Double(product.quantity)),
                            bold: true,
                            align: TeyaAlign.right
                        ),
                    ]
                )
            )
        }
        
        rows.append(TeyaReceiptRowDivider.shared)

        rows.append(
            TeyaReceiptRowItems(
                items: [
                    TeyaRowElementText(text: "TIP", bold: true, align: TeyaAlign.left),
                    TeyaRowElementText(text: PriceUtils.formatPrice(tip), bold: true, align: TeyaAlign.right),
                ]
            )
        )

        rows.append(
            TeyaReceiptRowItems(
                items: [
                    TeyaRowElementText(text: "TOTAL", bold: true, align: TeyaAlign.left),
                    TeyaRowElementText(text: PriceUtils.formatPrice(total), bold: true, align: TeyaAlign.right),
                ]
            )
        )
        
        rows.append(
            TeyaReceiptRowItem(item: TeyaRowElementQrCode(url: "https://teya.com", align: TeyaAlign.center))
        )
        
        rows.append(TeyaReceiptRowSpacer.shared)
        rows.append(TeyaReceiptRowSpacer.shared)
        
        rows.append(
            TeyaReceiptRowItem(
                item: TeyaRowElementText(text: "Thank you", bold: true, align: TeyaAlign.center)
            )
        )
        
        return TeyaTemplate(rows: rows)
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
            printModel: buildTableBillTemplate(tab: tab, items: billItems, totalMinor: Int(totalAmountMinor)),
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
        subscription.subscribe(listener: PaymentStateChangeListener())
    }

    func subscribeToTabEvents(_ listener: TeyaTabEventListener) {
        teyaPosLinkSDK.tabsApi.tabEvents.subscribe(tabEventListener: listener)
    }

    func unsubscribeFromTabEvents(_ listener: TeyaTabEventListener) {
        teyaPosLinkSDK.tabsApi.tabEvents.unsubscribe(tabEventListener: listener)
    }

    private func buildTableBillTemplate(tab: TeyaTabSummary, items: [Product], totalMinor: Int) -> TeyaTemplate {
        var rows: [TeyaReceiptRow] = []

        rows.append(
            TeyaReceiptRowItem(item: TeyaRowElementText(text: "BILL", bold: true, align: TeyaAlign.center))
        )
        rows.append(
            TeyaReceiptRowItem(item: TeyaRowElementText(text: tab.tabName, bold: false, align: TeyaAlign.center))
        )
        rows.append(TeyaReceiptRowSpacer.shared)
        rows.append(TeyaReceiptRowDivider.shared)

        for product in items {
            rows.append(
                TeyaReceiptRowItems(
                    items: [
                        TeyaRowElementText(
                            text: "\(product.quantity)x \(product.name.uppercased())",
                            bold: false,
                            align: TeyaAlign.left
                        ),
                        TeyaRowElementText(
                            text: PriceUtils.formatPrice(product.price * Double(product.quantity)),
                            bold: false,
                            align: TeyaAlign.right
                        ),
                    ]
                )
            )
        }

        rows.append(TeyaReceiptRowDivider.shared)
        rows.append(
            TeyaReceiptRowItems(
                items: [
                    TeyaRowElementText(text: "TOTAL", bold: true, align: TeyaAlign.left),
                    TeyaRowElementText(text: PriceUtils.formatMinor(totalMinor), bold: true, align: TeyaAlign.right),
                ]
            )
        )

        return TeyaTemplate(rows: rows)
    }
}

private class PaymentStateChangeListener: TeyaPaymentStateChangeListener {
    func onPaymentStateChanged(state: TeyaPaymentStateDetails) {
        print("Payment state changed: \(state)")
    }
}

private final class PrintingStatusSubscriptionListener: TeyaPrintingStatusSubscriptionListener {
    func onPrintingStateChanged(printStateDetails: TeyaPrintStateDetails) {
        print("Printing state changed: \(printStateDetails)")
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
