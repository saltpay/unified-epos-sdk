import SwiftUI
import Lemonade
import TeyaUnifiedEposSDK

struct TableDetailsView: View {
    @Bindable var viewModel: TablesViewModel

    var body: some View {
        guard let selectedTab = viewModel.selectedTab else {
            return AnyView(EmptyView())
        }
        let items = viewModel.selectedTabItems
        let tabDetail = viewModel.selectedTabDetail

        return AnyView(
            VStack(spacing: 0) {
                if let terminalId = selectedTab.showingBillTerminalId {
                    BillShowingBanner(terminalId: terminalId)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                }

                CurrentItems(
                    items: items,
                    onAdd: { viewModel.addProduct($0) },
                    onRemove: { viewModel.removeProduct($0) }
                )
                .padding(.horizontal, 16)
                .padding(.vertical, 8)

                HStack(spacing: 12) {
                    Spacer()
                    LemonadeUi.Button(
                        label: "Close table",
                        onClick: { viewModel.closeTab(selectedTab.tabId) },
                        variant: .critical,
                        type: .subtle,
                        size: .large
                    )
                    LemonadeUi.Button(
                        label: "Add items",
                        onClick: { viewModel.showProductCatalogue = true },
                        size: .large
                    )
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)

                Divider()

                if let tabDetail {
                    PaymentsSummary(
                        tab: tabDetail,
                        onViewPayments: { viewModel.showPaymentsDialog = true }
                    )
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }

                HStack {
                    LemonadeUi.Text("Total", textStyle: LemonadeTypography.shared.headingXSmall)
                    Spacer()
                    LemonadeUi.Text(
                        PriceUtils.formatMinor(viewModel.tabTotalMinor(selectedTab.tabId.value)),
                        textStyle: LemonadeTypography.shared.headingXSmall,
                        color: LemonadeTheme.colors.content.contentBrand
                    )
                }
                .padding(16)
            }
            .lemonadeTopBar(
                label: selectedTab.tabName,
                navigationAction: NavigationAction(action: .back, onAction: {})
            ) {
                ToolbarItem(placement: .topBarTrailing) {
                    StatusTag(status: selectedTab.status)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    LemonadeUi.IconButton(
                        icon: .arrowsRepeat,
                        contentDescription: "Refresh tab details",
                        onClick: { viewModel.refreshSelectedTabDetail() }
                    )
                }
            }
            .sheet(isPresented: $viewModel.showProductCatalogue) {
                ProductCatalogueSheet(
                    basket: items,
                    onAdd: { viewModel.addProduct($0) },
                    onRemove: { viewModel.removeProduct($0) },
                    onDone: { viewModel.showProductCatalogue = false }
                )
            }
            .sheet(isPresented: $viewModel.showPaymentsDialog) {
                if let tabDetail {
                    PaymentsSheet(tab: tabDetail, onDone: { viewModel.showPaymentsDialog = false })
                }
            }
        )
    }
}

private struct BillShowingBanner: View {
    let terminalId: String

    var body: some View {
        HStack(spacing: 8) {
            LemonadeUi.Text(
                "Bill showing on terminal ID",
                textStyle: LemonadeTypography.shared.bodyMediumRegular,
                color: LemonadeTheme.colors.content.contentSecondary
            )
            LemonadeUi.Tag(label: terminalId, voice: .info)
            Spacer()
        }
    }
}

private struct CurrentItems: View {
    let items: [Product]
    let onAdd: (Product) -> Void
    let onRemove: (Product) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            LemonadeUi.Text("Current items", textStyle: LemonadeTypography.shared.headingXSmall)
            if items.isEmpty {
                LemonadeUi.Text(
                    "No items yet. Tap \"Add items\" to get started.",
                    textStyle: LemonadeTypography.shared.bodyMediumRegular,
                    color: LemonadeTheme.colors.content.contentSecondary
                )
            } else {
                ScrollView {
                    VStack(spacing: 4) {
                        ForEach(items) { product in
                            HStack {
                                LemonadeUi.Text(
                                    "\(product.emoji) \(product.name)",
                                    textStyle: LemonadeTypography.shared.bodyMediumRegular
                                )
                                .frame(maxWidth: .infinity, alignment: .leading)

                                Button { onRemove(product) } label: {
                                    LemonadeUi.Text(
                                        "−",
                                        textStyle: LemonadeTypography.shared.headingSmall,
                                        color: LemonadeTheme.colors.content.contentBrand
                                    )
                                }
                                LemonadeUi.Text(
                                    "\(product.quantity)",
                                    textStyle: LemonadeTypography.shared.bodyMediumRegular
                                )
                                Button { onAdd(product) } label: {
                                    LemonadeUi.Text(
                                        "+",
                                        textStyle: LemonadeTypography.shared.headingSmall,
                                        color: LemonadeTheme.colors.content.contentBrand
                                    )
                                }
                                LemonadeUi.Text(
                                    PriceUtils.formatPrice(product.price * Double(product.quantity)),
                                    textStyle: LemonadeTypography.shared.bodyMediumRegular
                                )
                                .padding(.leading, 8)
                            }
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct PaymentsSummary: View {
    let tab: TeyaTab
    let onViewPayments: () -> Void

    private var payments: [TeyaPaymentRequestSummary] { tab.paymentRequests ?? [] }
    private var ongoing: TeyaPaymentRequestSummary? { payments.first { !$0.status.isFinal } }

    var body: some View {
        VStack(spacing: 8) {
            AmountRow(label: "Paid", amountMinor: tab.totalPaid?.asInt ?? 0)

            if let ongoing {
                HStack {
                    LemonadeUi.Text(
                        "Payment in progress: \(PriceUtils.formatMinor(Int(ongoing.amount)))",
                        textStyle: LemonadeTypography.shared.bodyMediumRegular,
                        color: LemonadeTheme.colors.content.contentInfo
                    )
                    Spacer()
                    PaymentStatusTag(state: ongoing.status)
                }
            }

            HStack {
                LemonadeUi.Text(
                    payments.isEmpty ? "No payments yet" : "Payments (\(payments.count))",
                    textStyle: LemonadeTypography.shared.bodyMediumRegular,
                    color: LemonadeTheme.colors.content.contentSecondary
                )
                Spacer()
                if !payments.isEmpty {
                    LemonadeUi.Button(
                        label: "View payments",
                        onClick: onViewPayments,
                        variant: .neutral,
                        type: .ghost,
                        size: .small
                    )
                }
            }
        }
    }
}

private struct AmountRow: View {
    let label: String
    let amountMinor: Int

    var body: some View {
        HStack {
            LemonadeUi.Text(label, textStyle: LemonadeTypography.shared.bodyMediumRegular)
            Spacer()
            LemonadeUi.Text(
                PriceUtils.formatMinor(amountMinor),
                textStyle: LemonadeTypography.shared.bodyMediumRegular
            )
        }
    }
}

private struct ProductCatalogueSheet: View {
    let basket: [Product]
    let onAdd: (Product) -> Void
    let onRemove: (Product) -> Void
    let onDone: () -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                ProductGrid(
                    products: Product.catalog(),
                    basket: basket,
                    onAdd: onAdd,
                    onRemove: onRemove
                )
                LemonadeUi.Button(label: "Done", onClick: onDone, size: .large)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
            }
            .navigationTitle("Add items")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

private struct PaymentsSheet: View {
    let tab: TeyaTab
    let onDone: () -> Void

    private var payments: [TeyaPaymentRequestSummary] { tab.paymentRequests ?? [] }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                AmountRow(label: "Paid", amountMinor: tab.totalPaid?.asInt ?? 0)
                if let remaining = tab.remaining?.asInt {
                    AmountRow(label: "Remaining", amountMinor: remaining)
                }
                Divider()
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(Array(payments.enumerated()), id: \.offset) { _, payment in
                            PaymentRow(payment: payment)
                        }
                    }
                }
            }
            .padding(16)
            .navigationTitle("Payments")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", action: onDone)
                }
            }
        }
    }
}

private struct PaymentRow: View {
    let payment: TeyaPaymentRequestSummary

    private var descriptor: String {
        [payment.method?.name, payment.type.name]
            .compactMap { $0?.capitalized }
            .joined(separator: " · ")
    }

    private var timestampMillis: Int64? {
        if payment.method == TeyaTabPaymentMethod.card {
            return payment.transactionTimestampEpochMillis?.asInt64
        }
        return payment.updatedAtEpochMillis?.asInt64
    }

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 2) {
                LemonadeUi.Text(
                    PriceUtils.formatMinor(Int(payment.amount)),
                    textStyle: LemonadeTypography.shared.bodyMediumSemiBold
                )
                if !descriptor.isEmpty {
                    LemonadeUi.Text(
                        descriptor,
                        textStyle: LemonadeTypography.shared.bodySmallRegular,
                        color: LemonadeTheme.colors.content.contentSecondary
                    )
                }
                if let tip = payment.tip?.asInt, tip > 0 {
                    LemonadeUi.Text(
                        "Tip: \(PriceUtils.formatMinor(tip))",
                        textStyle: LemonadeTypography.shared.bodySmallRegular,
                        color: LemonadeTheme.colors.content.contentSecondary
                    )
                }
                if let millis = timestampMillis {
                    LemonadeUi.Text(
                        Self.formatTimestamp(millis),
                        textStyle: LemonadeTypography.shared.bodySmallRegular,
                        color: LemonadeTheme.colors.content.contentSecondary
                    )
                }
            }
            Spacer()
            PaymentStatusTag(state: payment.status)
        }
    }

    private static let timestampFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd/MM/yy · HH:mm"
        return formatter
    }()

    private static func formatTimestamp(_ epochMillis: Int64) -> String {
        timestampFormatter.string(from: Date(timeIntervalSince1970: Double(epochMillis) / 1000.0))
    }
}
