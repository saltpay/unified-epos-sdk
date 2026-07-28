package com.example.eposposlinkexample.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.math.BigDecimal;

public class ProductItem {

    private final Product product;
    private final IntegerProperty quantity = new SimpleIntegerProperty(0);

    public ProductItem(Product product) {
        this.product = product;
    }

    public Product product() {
        return product;
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    public int getQuantity() {
        return quantity.get();
    }

    public void add() {
        quantity.set(quantity.get() + 1);
    }

    public void remove() {
        if (quantity.get() > 0) {
            quantity.set(quantity.get() - 1);
        }
    }

    public BigDecimal lineTotal() {
        return product.price().multiply(BigDecimal.valueOf(quantity.get()));
    }
}
