package com.example.orderprocessing.exception;

/**
 * Exception thrown when there is insufficient stock to fulfill an order
 */
public class InsufficientStockException extends RuntimeException {

    private final String productId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientStockException(String productId, int requestedQuantity, int availableQuantity) {
        super(String.format("Insufficient stock for product %s. Requested: %d, Available: %d",
                productId, requestedQuantity, availableQuantity));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
