package com.example.orderprocessing.exception;

/**
 * Exception thrown when a product is not found in the inventory
 */
public class ProductNotFoundException extends RuntimeException {

    private final String productId;

    public ProductNotFoundException(String productId) {
        super(String.format("Product not found: %s", productId));
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
