import Foundation

enum PriceUtils {
    static let currencyCode = "GBP"

    static var currencySymbol: String {
        Locale(identifier: "en_GB").currencySymbol ?? "£"
    }

    private static let priceFormatter: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = currencyCode
        formatter.locale = Locale(identifier: "en_GB")
        return formatter
    }()

    static func formatPrice(_ amount: Double) -> String {
        priceFormatter.string(from: NSNumber(value: amount)) ?? "£0.00"
    }

    static func isValidTipInput(_ input: String) -> Bool {
        if input.isEmpty { return true }
        // Accept both '.' and ',' as decimal separator
        let normalized = input.replacingOccurrences(of: ",", with: ".")
        let dotIndex = normalized.firstIndex(of: ".")
        if dotIndex != normalized.lastIndex(of: ".") { return false }
        for c in normalized {
            if c != "." && !c.isNumber { return false }
        }
        if let dotIdx = dotIndex {
            let decimals = normalized.distance(from: normalized.index(after: dotIdx), to: normalized.endIndex)
            if decimals > 2 { return false }
        }
        return true
    }
}
