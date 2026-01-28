package com.ecommerce.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockUpdateRequest(
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    Integer amount,

    @NotNull(message = "Operation is required")
    StockOperation operation
) {
    public enum StockOperation {
        INCREMENT,
        DECREMENT
    }
}
