import SwiftUI
import Lemonade

struct ProductCard: View {
    let product: Product
    let count: Int
    let onAdd: () -> Void
    let onRemove: () -> Void

    var body: some View {
        LemonadeUi.Card(contentPadding: .medium, background: .elevated) {
            VStack(alignment: .leading, spacing: 4) {
                LemonadeUi.Text(product.emoji, textStyle: LemonadeTypography.shared.headingMedium)
                LemonadeUi.Text(product.name, textStyle: LemonadeTypography.shared.headingXSmall)
                LemonadeUi.Text(
                    PriceUtils.formatPrice(product.price),
                    textStyle: LemonadeTypography.shared.bodyMediumRegular,
                    color: LemonadeTheme.colors.content.contentSecondary
                )

                HStack {
                    if count > 0 {
                        LemonadeUi.Text(
                            "x\(count)",
                            textStyle: LemonadeTypography.shared.bodyMediumSemiBold,
                            color: LemonadeTheme.colors.content.contentBrand
                        )
                        Spacer()
                        LemonadeUi.Button(
                            label: "Remove",
                            onClick: onRemove,
                            variant: .neutral,
                            type: .ghost,
                            size: .small
                        )
                    }
                }
                .frame(height: 44)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: onAdd)
    }
}
