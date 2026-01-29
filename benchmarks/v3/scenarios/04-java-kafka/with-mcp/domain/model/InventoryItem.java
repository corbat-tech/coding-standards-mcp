package com.example.order.domain.model;

import java.util.Objects;

/**
 * Domain entity representing an inventory item.
 * Encapsulates stock management with business rules.
 */
public class InventoryItem {

    private final String productId;
    private final String productName;
    private int availableQuantity;
    private int reservedQuantity;

    public InventoryItem(String productId, String productName, int availableQuantity) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.productName = Objects.requireNonNull(productName, "productName must not be null");

        if (availableQuantity < 0) {
            throw new IllegalArgumentException("Available quantity cannot be negative");
        }
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getTotalStock() {
        return availableQuantity + reservedQuantity;
    }

    /**
     * Reserves stock for an order.
     *
     * @param quantity the quantity to reserve
     * @return true if reservation successful, false if insufficient stock
     */
    public boolean reserveStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to reserve must be positive");
        }
        if (availableQuantity < quantity) {
            return false;
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
        return true;
    }

    /**
     * Releases previously reserved stock.
     *
     * @param quantity the quantity to release
     */
    public void releaseReservedStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to release must be positive");
        }
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more than reserved quantity");
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    /**
     * Confirms reserved stock as sold (removes from reserved).
     *
     * @param quantity the quantity to confirm
     */
    public void confirmReservedStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to confirm must be positive");
        }
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot confirm more than reserved quantity");
        }
        reservedQuantity -= quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItem that = (InventoryItem) o;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }
}
