import SwiftUI

struct BottomBar: View {
    let itemCount: Int
    let subtotal: Double
    let total: Double
    @Binding var tipInput: String
    let onPay: () -> Void
    let onPrint: () -> Void
    let payEnabled: Bool

    var body: some View {
        VStack(spacing: 0) {
            Divider()

            VStack(spacing: 12) {
                HStack {
                    Text("\(itemCount) item\(itemCount != 1 ? "s" : "")")
                    Spacer()
                    Text("Subtotal: \(PriceUtils.formatPrice(subtotal))")
                }
                .font(.body)

                TextField("Tip (\(PriceUtils.currencySymbol))", text: $tipInput)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(.roundedBorder)
                    .onChange(of: tipInput) { _, newValue in
                        if !PriceUtils.isValidTipInput(newValue) {
                            tipInput = String(newValue.dropLast())
                        }
                    }

                HStack(spacing: 8) {
                    Button("Print") {
                        onPrint()
                    }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .disabled(!payEnabled)

                    Button("Pay \(PriceUtils.formatPrice(total))") {
                        onPay()
                    }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .disabled(!payEnabled)
                }
            }
            .padding(16)
        }
        .background(Color(.systemBackground))
    }
}
