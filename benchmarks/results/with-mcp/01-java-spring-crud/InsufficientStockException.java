package com.ecommerce.domain.exception;

public class InsufficientStockException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Insufficient stock for product %d. Available: %d, Requested: %d";

    public InsufficientStockException(Long productId, Integer available, Integer requested) {
        super(String.format(MESSAGE_TEMPLATE, productId, available, requested));
    }
}
