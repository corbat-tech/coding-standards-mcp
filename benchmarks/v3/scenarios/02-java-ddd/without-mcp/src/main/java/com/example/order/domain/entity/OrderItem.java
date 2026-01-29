package com.example.order.domain.entity;

import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.ProductId;
import com.example.order.domain.valueobject.Quantity;

import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing an item within an Order.
 * OrderItem is an entity because it has identity within the Order aggregate
 * and can be individually referenced and modified.
 */
public class OrderItem {

    private final UUID id;
    private final ProductId productId;
    private final String productName;
    private final Money unitPrice;
    private Quantity quantity;

    public OrderItem(ProductId productId, String productName, Money unitPrice, Quantity quantity) {
        this.id = UUID.randomUUID();
        this.productId = Objects.requireNonNull(productId, "ProductId cannot be null");
        this.productName = Objects.requireNonNull(productName, "ProductName cannot be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "UnitPrice cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");

        if (productName.isBlank()) {
            throw new IllegalArgumentException("ProductName cannot be blank");
        }
    }

    public UUID getId() {
        return id;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    /**
     * Calculates the total price for this item (unit price * quantity).
     *
     * @return the total price for this item
     */
    public Money getTotalPrice() {
        return unitPrice.multiply(quantity.getValue());
    }

    /**
     * Updates the quantity of this item.
     *
     * @param newQuantity the new quantity
     */
    void updateQuantity(Quantity newQuantity) {
        this.quantity = Objects.requireNonNull(newQuantity, "Quantity cannot be null");
    }

    /**
     * Increases the quantity by the specified amount.
     *
     * @param additionalQuantity the quantity to add
     */
    void increaseQuantity(Quantity additionalQuantity) {
        this.quantity = this.quantity.add(additionalQuantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", unitPrice=" + unitPrice +
                ", quantity=" + quantity +
                '}';
    }
}
