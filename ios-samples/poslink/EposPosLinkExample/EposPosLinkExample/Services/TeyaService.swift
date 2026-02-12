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
        let paymentSubscription = teyaPosLinkSDK.transactionsApi?.makePayment(
            transactionId: UUID().uuidString, // or pass whatever identifier you already have for the payment you're about to make
            amount: totalMinorUnits, // the total amount to be paid including the tip, in the smallest unit of the currency (e.g., cents).
            currency: PriceUtils.currencyCode, // The ISO 4217 currency code (e.g., "GBP", "EUR").
            tip: tipMinorUnits.toKotlinInt(), // An optional tip amount, in the smallest unit of the currency.
            purchaseData: nil
        )
        
        paymentSubscription?.subscribe(listener: PaymentStateChangeListener())
        paymentSubscription?.subscribe(
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
        teyaPosLinkSDK.printingApi?.printCustomTemplate(template: template).subscribe(
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
        
        // Title + date row
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
        
        // Product items
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
        
        // Tip
        rows.append(
            TeyaReceiptRowItems(
                items: [
                    TeyaRowElementText(text: "TIP", bold: true, align: TeyaAlign.left),
                    TeyaRowElementText(text: PriceUtils.formatPrice(tip), bold: true, align: TeyaAlign.right),
                ]
            )
        )
        
        // Total
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
}

// MARK: - Payment Listener
private class PaymentStateChangeListener: TeyaPaymentStateChangeListener {
    func onPaymentStateChanged(state: TeyaPaymentStateDetails) {
        print("Payment state changed: \(state)")
        
        if state.isFinal {
            if state.state == TeyaPaymentState.successful {
                // Persist the gatewayPaymentId to refund later on
            }
        }
    }
}

// MARK: - Print Listener
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
