package com.example.products.application;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Command for updating an existing product.
 */
public record UpdateProductCommand(
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
) {}
