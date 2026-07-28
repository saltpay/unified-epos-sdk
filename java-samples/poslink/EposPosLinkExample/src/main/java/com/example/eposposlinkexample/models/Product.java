package com.example.eposposlinkexample.models;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class Product {
    private final int id;
    private final String name;
    private final BigDecimal price;

    Product(int id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public BigDecimal price() {
        return price;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Product) obj;
        return this.id == that.id &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price);
    }

    @Override
    public String toString() {
        return "Product[" +
                "id=" + id + ", " +
                "name=" + name + ", " +
                "price=" + price + ']';
    }

    public static List<Product> getProducts() {
        return List.of(
                new Product(1, "Apple", new BigDecimal("0.99")),
                new Product(2, "Banana", new BigDecimal("0.59")),
                new Product(3, "Orange", new BigDecimal("0.79")),
                new Product(4, "Grapes", new BigDecimal("2.49")),
                new Product(5, "Mango", new BigDecimal("1.99")),
                new Product(6, "Peach", new BigDecimal("1.29")),
                new Product(7, "Lemon", new BigDecimal("1.29")),
                new Product(8, "Lime", new BigDecimal("1.29")),
                new Product(9, "Strawberry", new BigDecimal("1.29")),
                new Product(10, "Watermelon", new BigDecimal("1.29"))
        );
    }
}
