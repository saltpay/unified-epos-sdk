package com.example.eposappexample.poslink.models

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val quantity: Int = 0
) {
    companion object {
        fun getProducts(): List<Product> {
            return listOf(
                Product(1, "Apple", 0.99),
                Product(2, "Banana", 0.59),
                Product(3, "Orange", 0.79),
                Product(4, "Grapes", 2.49),
                Product(5, "Mango", 1.99),
                Product(6, "Peach", 1.29),
                Product(7, "Lemon", 1.29),
                Product(8, "Lime", 1.29),
                Product(9, "Strawberry", 1.29),
                Product(10, "Watermelon", 1.29),
            )
        }
    }
}

