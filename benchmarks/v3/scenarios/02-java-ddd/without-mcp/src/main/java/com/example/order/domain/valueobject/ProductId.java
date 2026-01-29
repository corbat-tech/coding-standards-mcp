package com.example.order.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Product identifier.
 * Immutable and validated upon creation.
 */
public final class ProductId {

    private final UUID value;

    private ProductId(UUID value) {
        this.value = Objects.requireNonNull(value, "ProductId value cannot be null");
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId of(UUID value) {
        return new ProductId(value);
    }

    public static ProductId of(String value) {
        Objects.requireNonNull(value, "ProductId string value cannot be null");
        return new ProductId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductId productId = (ProductId) o;
        return Objects.equals(value, productId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
