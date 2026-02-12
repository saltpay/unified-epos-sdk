import SwiftUI

struct ProductCard: View {
    let product: Product
    let count: Int
    let onAdd: () -> Void
    let onRemove: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(product.name)
                .font(.headline)

            Text(PriceUtils.formatPrice(product.price))
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Spacer().frame(height: 4)

            HStack {
                Text("x\(count)")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundStyle(.tint)

                Spacer()

                Button("Remove") {
                    onRemove()
                }
                .font(.caption)
                .disabled(count == 0)
            }
            .opacity(count > 0 ? 1 : 0)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.secondarySystemBackground))
        )
        .contentShape(RoundedRectangle(cornerRadius: 12))
        .onTapGesture {
            onAdd()
        }
    }
}
