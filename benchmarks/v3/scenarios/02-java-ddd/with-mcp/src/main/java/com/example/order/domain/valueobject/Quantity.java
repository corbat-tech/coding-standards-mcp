package com.example.order.domain.valueobject;

import com.example.order.domain.exception.InvalidQuantityException;
import java.util.Objects;

public final class Quantity {

    public static final Quantity ONE = new Quantity(1);

    private final int value;

    private Quantity(int value) {
        this.value = value;
    }

    public static Quantity of(int value) {
        if (value <= 0) {
            throw new InvalidQuantityException("Quantity must be positive");
        }
        return new Quantity(value);
    }

    public int getValue() {
        return value;
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
