package com.example.saga.domain.valueobject;

/**
 * Represents the status of an order throughout the fulfillment process.
 */
public enum OrderStatus {
    PENDING,
    CREATED,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSED,
    SHIPPED,
    CANCELLED,
    FAILED
}
