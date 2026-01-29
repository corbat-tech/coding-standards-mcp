package com.example.order.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing an Order identifier.
 * Immutable and validated upon creation.
 */
public final class OrderId {

    private final UUID value;

    private OrderId(UUID value) {
        this.value = Objects.requireNonNull(value, "OrderId value cannot be null");
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    public static OrderId of(String value) {
        Objects.requireNonNull(value, "OrderId string value cannot be null");
        return new OrderId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(value, orderId.value);
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
