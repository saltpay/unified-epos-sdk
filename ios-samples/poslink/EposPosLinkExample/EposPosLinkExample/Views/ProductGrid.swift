import SwiftUI

struct ProductGrid: View {
    let products: [Product]
    let basket: [Product]
    let onAdd: (Product) -> Void
    let onRemove: (Product) -> Void

    private let columns = [GridItem(.adaptive(minimum: 150), spacing: 8)]

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(products) { product in
                    let count = basket.first(where: { $0.id == product.id })?.quantity ?? 0
                    ProductCard(
                        product: product,
                        count: count,
                        onAdd: { onAdd(product) },
                        onRemove: { onRemove(product) }
                    )
                }
            }
            .padding(12)
        }
    }
}
