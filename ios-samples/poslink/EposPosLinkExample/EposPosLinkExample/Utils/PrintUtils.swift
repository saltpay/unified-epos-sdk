import Foundation
import TeyaUnifiedEposSDK

enum PrintUtils {
    static func buildCustomPrintTemplate(products: [Product], tip: Double) -> TeyaTemplate {
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

    static func buildTableBillTemplate(tab: TeyaTabSummary, items: [Product], totalMinor: Int) -> TeyaTemplate {
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

