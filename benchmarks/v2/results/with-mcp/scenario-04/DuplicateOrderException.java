package com.example.order.domain.exception;

public class DuplicateOrderException extends OrderProcessingException {
    public DuplicateOrderException(String orderId) {
        super(orderId, "Order already processed: " + orderId, false);
    }
}
