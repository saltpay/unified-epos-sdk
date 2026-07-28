package com.example.eposposlinkexample.models;

import java.math.BigDecimal;
import java.util.List;

public record Product(int id, String name, BigDecimal price, String emoji) {

    public static List<Product> getProducts() {
        return List.of(
                new Product(1, "Apple", new BigDecimal("0.99"), "🍎"),
                new Product(2, "Banana", new BigDecimal("0.59"), "🍌"),
                new Product(3, "Orange", new BigDecimal("0.79"), "🍊"),
                new Product(4, "Grapes", new BigDecimal("2.49"), "🍇"),
                new Product(5, "Mango", new BigDecimal("1.99"), "🥭"),
                new Product(6, "Peach", new BigDecimal("1.29"), "🍑"),
                new Product(7, "Lemon", new BigDecimal("1.29"), "🍋"),
                new Product(8, "Lime", new BigDecimal("1.29"), "🟢"),
                new Product(9, "Strawberry", new BigDecimal("1.29"), "🍓"),
                new Product(10, "Watermelon", new BigDecimal("1.29"), "🍉")
        );
    }
}
