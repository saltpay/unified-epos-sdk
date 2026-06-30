import Foundation

struct Product: Identifiable, Equatable {
    let id: Int
    let name: String
    let price: Double
    let emoji: String
    var quantity: Int

    init(id: Int, name: String, price: Double, emoji: String, quantity: Int = 0) {
        self.id = id
        self.name = name
        self.price = price
        self.emoji = emoji
        self.quantity = quantity
    }

    static func catalog() -> [Product] {
        [
            Product(id: 1, name: "Apple", price: 0.99, emoji: "🍎"),
            Product(id: 2, name: "Banana", price: 0.59, emoji: "🍌"),
            Product(id: 3, name: "Orange", price: 0.79, emoji: "🍊"),
            Product(id: 4, name: "Grapes", price: 2.49, emoji: "🍇"),
            Product(id: 5, name: "Mango", price: 1.99, emoji: "🥭"),
            Product(id: 6, name: "Peach", price: 1.29, emoji: "🍑"),
            Product(id: 7, name: "Lemon", price: 1.29, emoji: "🍋"),
            Product(id: 8, name: "Lime", price: 1.29, emoji: "🟢"),
            Product(id: 9, name: "Strawberry", price: 1.29, emoji: "🍓"),
            Product(id: 10, name: "Watermelon", price: 1.29, emoji: "🍉"),
        ]
    }
}
