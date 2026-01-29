package domain.valueobject;

import domain.exception.InvalidQuantityException;

import java.util.Objects;

/**
 * Value Object representing a quantity of items.
 * Immutable and always positive.
 */
public final class Quantity {

    private final int value;

    private Quantity(int value) {
        this.value = value;
    }

    public static Quantity of(int value) {
        if (value <= 0) {
            throw new InvalidQuantityException(
                "Quantity must be positive, got: " + value
            );
        }
        return new Quantity(value);
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
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
