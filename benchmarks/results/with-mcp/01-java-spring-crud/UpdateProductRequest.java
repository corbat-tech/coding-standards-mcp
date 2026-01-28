package com.ecommerce.application.dto;

import com.ecommerce.domain.model.ProductCategory;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductRequest(
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @Positive(message = "Price must be greater than zero")
    BigDecimal price,

    @PositiveOrZero(message = "Stock must be zero or positive")
    Integer stock,

    ProductCategory category
) {}
