import SwiftUI
import Lemonade

struct MainView: View {
    @State private var viewModel = MainViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                ProductGrid(
                    products: Product.catalog(),
                    basket: viewModel.basket,
                    onAdd: { viewModel.addProduct($0) },
                    onRemove: { viewModel.removeProduct($0) }
                )

                BottomBar(
                    itemCount: viewModel.itemCount,
                    subtotal: viewModel.subtotal,
                    total: viewModel.total,
                    tipInput: $viewModel.tipInput,
                    onPay: { viewModel.pay() },
                    onPrint: { viewModel.printReceipt() },
                    payEnabled: viewModel.payEnabled,
                    unreferencedRefund: viewModel.unreferencedRefund,
                    onUnreferencedRefundChange: { viewModel.updateUnreferencedRefund($0) }
                )
            }
            .lemonadeTopBar(label: "ePOS Sample Poslink") {
                TopBar(
                    onClearUserAuth: { viewModel.clearUserAuth() },
                    onClearDeviceLink: { viewModel.clearDeviceLink() }
                )
            }
            .toolbar {
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") {
                        UIApplication.shared.sendAction(
                            #selector(UIResponder.resignFirstResponder),
                            to: nil, from: nil, for: nil
                        )
                    }
                }
            }
        }
    }
}
