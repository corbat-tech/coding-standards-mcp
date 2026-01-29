package com.example.orderprocessing.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing inventory stock for a product
 */
@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @Column(name = "product_id", nullable = false, updatable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private int quantityReserved = 0;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Reserve stock for an order
     * @param quantity The quantity to reserve
     * @return true if reservation was successful, false if insufficient stock
     */
    public boolean reserveStock(int quantity) {
        int available = quantityAvailable - quantityReserved;
        if (available >= quantity) {
            this.quantityReserved += quantity;
            return true;
        }
        return false;
    }

    /**
     * Commit reserved stock (after order is confirmed/shipped)
     * @param quantity The quantity to commit
     */
    public void commitReservedStock(int quantity) {
        this.quantityAvailable -= quantity;
        this.quantityReserved -= quantity;
    }

    /**
     * Release reserved stock (if order is cancelled)
     * @param quantity The quantity to release
     */
    public void releaseReservedStock(int quantity) {
        this.quantityReserved -= quantity;
    }

    /**
     * Get the actual available quantity (total - reserved)
     */
    public int getEffectiveAvailable() {
        return quantityAvailable - quantityReserved;
    }
}
