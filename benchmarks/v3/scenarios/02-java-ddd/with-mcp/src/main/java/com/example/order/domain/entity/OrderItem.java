package com.example.order.domain.entity;

import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.Quantity;
import java.util.Objects;

public final class OrderItem {

    private final String productId;
    private final String productName;
    private final Money unitPrice;
    private final Quantity quantity;

    public OrderItem(String productId, String productName, Money unitPrice, Quantity quantity) {
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null");
        this.productName = Objects.requireNonNull(productName, "Product name cannot be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
    }

    public Money calculateTotal() {
        return unitPrice.multiply(quantity.getValue());
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Money getUnitPrice() { return unitPrice; }
    public Quantity getQuantity() { return quantity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return productId.equals(orderItem.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }
}
