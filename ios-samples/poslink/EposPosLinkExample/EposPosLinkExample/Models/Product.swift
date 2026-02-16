import Foundation

struct Product: Identifiable, Equatable {
    let id: Int
    let name: String
    let price: Double
    var quantity: Int

    init(id: Int, name: String, price: Double, quantity: Int = 0) {
        self.id = id
        self.name = name
        self.price = price
        self.quantity = quantity
    }

    static func catalog() -> [Product] {
        [
            Product(id: 1, name: "Apple", price: 0.99),
            Product(id: 2, name: "Banana", price: 0.59),
            Product(id: 3, name: "Orange", price: 0.79),
            Product(id: 4, name: "Grapes", price: 2.49),
            Product(id: 5, name: "Mango", price: 1.99),
            Product(id: 6, name: "Peach", price: 1.29),
            Product(id: 7, name: "Lemon", price: 1.29),
            Product(id: 8, name: "Lime", price: 1.29),
            Product(id: 9, name: "Strawberry", price: 1.29),
            Product(id: 10, name: "Watermelon", price: 1.29),
        ]
    }
}
