import SwiftUI
import Lemonade

struct BottomBar: View {
    let itemCount: Int
    let subtotal: Double
    let total: Double
    @Binding var tipInput: String
    let onPay: () -> Void
    let onPrint: () -> Void
    let payEnabled: Bool
    let unreferencedRefund: Bool
    let onUnreferencedRefundChange: (Bool) -> Void

    var body: some View {
        VStack(spacing: 0) {
            Divider()

            VStack(spacing: 12) {
                HStack {
                    LemonadeUi.Text(
                        "\(itemCount) item\(itemCount != 1 ? "s" : "")",
                        textStyle: LemonadeTypography.shared.bodyMediumRegular
                    )
                    Spacer()
                    LemonadeUi.Text(
                        "Subtotal: \(PriceUtils.formatPrice(subtotal))",
                        textStyle: LemonadeTypography.shared.bodyMediumRegular
                    )
                }

                LemonadeUi.TextField(
                    input: $tipInput,
                    onInputChanged: { newValue in
                        if !PriceUtils.isValidTipInput(newValue) {
                            tipInput = String(newValue.dropLast())
                        }
                    },
                    label: "Tip (\(PriceUtils.currencySymbol))",
                    placeholderText: "0.00"
                )

                HStack(spacing: 8) {
                    LemonadeUi.Checkbox(
                        status: unreferencedRefund ? .checked : .unchecked,
                        onCheckboxClicked: { onUnreferencedRefundChange(!unreferencedRefund) }
                    )
                    LemonadeUi.Text(
                        "Unreferenced refund",
                        textStyle: LemonadeTypography.shared.bodyMediumRegular
                    )
                    Spacer()
                }
                .contentShape(Rectangle())
                .onTapGesture { onUnreferencedRefundChange(!unreferencedRefund) }

                HStack(spacing: 8) {
                    LemonadeUi.Button(
                        label: "Print",
                        onClick: onPrint,
                        variant: .neutral,
                        type: .subtle,
                        size: .large,
                        enabled: payEnabled
                    )
                    .frame(maxWidth: .infinity)

                    LemonadeUi.Button(
                        label: unreferencedRefund
                            ? "Refund \(PriceUtils.formatPrice(subtotal))"
                            : "Pay \(PriceUtils.formatPrice(total))",
                        onClick: onPay,
                        size: .large,
                        enabled: payEnabled
                    )
                    .frame(maxWidth: .infinity)
                }
            }
            .padding(16)
        }
        .background(LemonadeTheme.colors.background.bgDefault)
    }
}
