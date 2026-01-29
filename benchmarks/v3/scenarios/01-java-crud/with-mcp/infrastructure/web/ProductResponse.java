package com.example.products.infrastructure.web;

import com.example.products.domain.Product;
import java.math.BigDecimal;

/**
 * Response DTO for product data.
 */
public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String category
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getCategory()
        );
    }
}
