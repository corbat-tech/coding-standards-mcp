package com.example.payment.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique payment identifier.
 */
public final class PaymentId {

    private final UUID value;

    private PaymentId(UUID value) {
        this.value = value;
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    public static PaymentId of(String value) {
        Objects.requireNonNull(value, "Payment ID cannot be null");
        return new PaymentId(UUID.fromString(value));
    }

    public static PaymentId of(UUID value) {
        Objects.requireNonNull(value, "Payment ID cannot be null");
        return new PaymentId(value);
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentId paymentId = (PaymentId) o;
        return Objects.equals(value, paymentId.value);
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
