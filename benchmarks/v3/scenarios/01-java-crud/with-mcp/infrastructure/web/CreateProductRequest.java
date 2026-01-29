package com.example.products.infrastructure.web;

import com.example.products.application.CreateProductCommand;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request DTO for creating a product.
 */
public record CreateProductRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    String name,

    @Size(max = 500)
    String description,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be positive")
    BigDecimal price,

    @NotBlank(message = "Category is required")
    String category
) {
    public CreateProductCommand toCommand() {
        return new CreateProductCommand(name, description, price, category);
    }
}
