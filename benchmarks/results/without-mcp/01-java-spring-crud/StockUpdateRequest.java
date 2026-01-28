package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;

public class StockUpdateRequest {

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    @NotNull(message = "Operation type is required")
    private StockOperation operation;

    public enum StockOperation {
        INCREMENT,
        DECREMENT
    }

    public StockUpdateRequest() {
    }

    public StockUpdateRequest(Integer quantity, StockOperation operation) {
        this.quantity = quantity;
        this.operation = operation;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public StockOperation getOperation() {
        return operation;
    }

    public void setOperation(StockOperation operation) {
        this.operation = operation;
    }
}
