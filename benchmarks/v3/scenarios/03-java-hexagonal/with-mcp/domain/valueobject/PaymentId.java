package com.payment.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for Payment aggregate.
 * Ensures type safety and prevents primitive obsession.
 */
public final class PaymentId {

    private final UUID value;

    private PaymentId(UUID value) {
        this.value = Objects.requireNonNull(value, "PaymentId value cannot be null");
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    public static PaymentId of(UUID value) {
        return new PaymentId(value);
    }

    public static PaymentId of(String value) {
        Objects.requireNonNull(value, "PaymentId string cannot be null");
        return new PaymentId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentId paymentId = (PaymentId) o;
        return value.equals(paymentId.value);
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
