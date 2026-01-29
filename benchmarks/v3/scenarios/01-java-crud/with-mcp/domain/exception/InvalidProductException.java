package com.example.products.domain.exception;

/**
 * Exception thrown when product data is invalid.
 */
public class InvalidProductException extends RuntimeException {

    private final String field;

    public InvalidProductException(String field, String message) {
        super("Invalid product " + field + ": " + message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
