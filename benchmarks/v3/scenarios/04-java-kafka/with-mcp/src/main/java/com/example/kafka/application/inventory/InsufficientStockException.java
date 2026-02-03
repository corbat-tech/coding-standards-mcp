package com.example.kafka.application.inventory;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productId, int requestedQuantity) {
        super("Insufficient stock for product " + productId + ", requested: " + requestedQuantity);
    }
}
