package com.example.order.domain.valueobject;

public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canAddItems() {
        return this == DRAFT;
    }

    public boolean canConfirm() {
        return this == DRAFT;
    }

    public boolean canShip() {
        return this == CONFIRMED;
    }

    public boolean canDeliver() {
        return this == SHIPPED;
    }

    public boolean canCancel() {
        return this == DRAFT || this == CONFIRMED;
    }
}
