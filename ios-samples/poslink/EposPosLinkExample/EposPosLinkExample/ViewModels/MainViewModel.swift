import Foundation

@Observable
final class MainViewModel {
    var basket: [Product] = []
    var tipInput: String = ""

    var tipAmount: Double {
        Double(tipInput.replacingOccurrences(of: ",", with: ".")) ?? 0.0
    }

    var subtotal: Double {
        basket.reduce(0.0) { $0 + $1.price * Double($1.quantity) }
    }

    var total: Double {
        subtotal + tipAmount
    }

    var itemCount: Int {
        basket.reduce(0) { $0 + $1.quantity }
    }

    var payEnabled: Bool {
        !basket.isEmpty
    }

    func addProduct(_ product: Product) {
        if let index = basket.firstIndex(where: { $0.id == product.id }) {
            basket[index].quantity += 1
        } else {
            var newProduct = product
            newProduct.quantity = 1
            basket.append(newProduct)
        }
    }

    func removeProduct(_ product: Product) {
        guard let index = basket.firstIndex(where: { $0.id == product.id }) else { return }
        if basket[index].quantity > 1 {
            basket[index].quantity -= 1
        } else {
            basket.remove(at: index)
        }
    }

    func updateTipInput(_ value: String) {
        tipInput = value
    }

    func pay() {
        let totalInMinorUnits = Int32((total * 100).rounded())
        let tipInMinorUnits = Int32((tipAmount * 100).rounded())
        TeyaService.shared.makePayment(totalMinorUnits: totalInMinorUnits, tipMinorUnits: tipInMinorUnits)
    }

    func printReceipt() {
        TeyaService.shared.printReceipt(products: basket, tip: tipAmount)
    }

    func clearUserAuth() {
        TeyaService.shared.clearUserAuth()
        TeyaService.shared.setUp()
    }

    func clearDeviceLink() {
        TeyaService.shared.clearDeviceLink()
        TeyaService.shared.setUp()
    }
}
