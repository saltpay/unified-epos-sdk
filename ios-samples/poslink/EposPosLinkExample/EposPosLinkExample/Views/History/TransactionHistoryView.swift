import SwiftUI
import Lemonade
import TeyaUnifiedEposSDK

struct TransactionHistoryView: View {
    private let store = TransactionStore.shared
    @State private var viewModel = TransactionHistoryViewModel()

    var body: some View {
        NavigationStack {
            Group {
                if store.transactions.isEmpty {
                    VStack {
                        Spacer()
                        LemonadeUi.Text(
                            "No transactions yet.\nComplete a sale to see it here.",
                            textStyle: LemonadeTypography.shared.bodyMediumRegular,
                            textAlign: .center,
                            color: LemonadeTheme.colors.content.contentSecondary
                        )
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                    .padding(24)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            ForEach(store.transactions) { record in
                                TransactionRow(
                                    record: record,
                                    refunding: viewModel.refundingIds.contains(record.id),
                                    onRefund: { viewModel.refund(record) }
                                )
                                Divider()
                            }
                        }
                    }
                }
            }
            .lemonadeTopBar(label: "Transaction History")
        }
    }
}

private struct TransactionRow: View {
    let record: TransactionRecord
    let refunding: Bool
    let onRefund: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    LemonadeUi.Text(
                        PriceUtils.formatMinor(record.amountMinor),
                        textStyle: LemonadeTypography.shared.headingXSmall
                    )
                    TransactionStatusTag(record: record)
                }
                LemonadeUi.Text(
                    Self.typeLabel(record.type),
                    textStyle: LemonadeTypography.shared.bodyMediumRegular,
                    color: LemonadeTheme.colors.content.contentSecondary
                )
                if let source = Self.sourceLabel(record) {
                    LemonadeUi.Text(
                        source,
                        textStyle: LemonadeTypography.shared.bodyMediumRegular,
                        color: LemonadeTheme.colors.content.contentSecondary
                    )
                }
                LemonadeUi.Text(
                    Self.formatTimestamp(record.timestamp),
                    textStyle: LemonadeTypography.shared.bodySmallRegular,
                    color: LemonadeTheme.colors.content.contentSecondary
                )
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if record.isRefundable {
                LemonadeUi.Button(
                    label: refunding ? "Refunding…" : "Refund",
                    onClick: onRefund,
                    variant: .neutral,
                    type: .solid,
                    size: .large,
                    enabled: !refunding
                )
                .fixedSize(horizontal: true, vertical: false)
            }
        }
        .padding(16)
    }

    private static func typeLabel(_ type: TeyaTransactionType) -> String {
        switch type {
        case .payment: return "Payment"
        case .refund: return "Refund"
        default: return "Unknown"
        }
    }

    private static func sourceLabel(_ record: TransactionRecord) -> String? {
        guard record.type == .payment else { return nil }
        if !record.isTabPayment { return "Card" }
        return record.isCashPayment ? "Cash - Table payment" : "Card - Table payment"
    }

    private static let timestampFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm • dd MMM yyyy"
        formatter.locale = Locale(identifier: "en_GB")
        return formatter
    }()

    private static func formatTimestamp(_ epochMillis: Int64) -> String {
        timestampFormatter.string(from: Date(timeIntervalSince1970: Double(epochMillis) / 1000.0))
    }
}

private struct TransactionStatusTag: View {
    let record: TransactionRecord

    private var label: String {
        guard record.isSuccess else { return "Failed" }
        return record.type == .refund ? "Refunded" : "Paid"
    }

    private var voice: TagVoice {
        guard record.isSuccess else { return .critical }
        return record.type == .refund ? .info : .positive
    }

    var body: some View {
        LemonadeUi.Tag(label: label, voice: voice)
    }
}
