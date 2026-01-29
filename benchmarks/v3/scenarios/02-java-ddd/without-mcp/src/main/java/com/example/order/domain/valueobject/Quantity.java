package com.example.order.domain.valueobject;

import java.util.Objects;

/**
 * Value Object representing a quantity.
 * Immutable and validated upon creation.
 * Ensures quantity is always positive.
 */
public final class Quantity {

    public static final Quantity ONE = Quantity.of(1);

    private final int value;

    private Quantity(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + value);
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public int getValue() {
        return value;
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(this.value - other.value);
    }

    public boolean isGreaterThan(Quantity other) {
        return this.value > other.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quantity quantity = (Quantity) o;
        return value == quantity.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
