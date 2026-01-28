package com.ecommerce.domain.exception;

public class ProductNotFoundException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Product not found with id: %d";

    public ProductNotFoundException(Long id) {
        super(String.format(MESSAGE_TEMPLATE, id));
    }
}
