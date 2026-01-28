package com.example.order.domain.exception;

public class OrderProcessingException extends RuntimeException {
    private final String orderId;
    private final boolean retryable;

    public OrderProcessingException(String orderId, String message, boolean retryable) {
        super(message);
        this.orderId = orderId;
        this.retryable = retryable;
    }

    public OrderProcessingException(String orderId, String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.orderId = orderId;
        this.retryable = retryable;
    }

    public String getOrderId() {
        return orderId;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
