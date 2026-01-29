package com.payment.domain.entity;

/**
 * Represents the lifecycle states of a Payment.
 * Follows state machine pattern with valid transitions.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED;

    public boolean canTransitionTo(PaymentStatus target) {
        return switch (this) {
            case PENDING -> target == PROCESSING || target == FAILED;
            case PROCESSING -> target == COMPLETED || target == FAILED;
            case COMPLETED -> target == REFUNDED || target == PARTIALLY_REFUNDED;
            case PARTIALLY_REFUNDED -> target == REFUNDED || target == PARTIALLY_REFUNDED;
            case FAILED, REFUNDED -> false;
        };
    }

    public boolean isTerminal() {
        return this == FAILED || this == REFUNDED;
    }

    public boolean isRefundable() {
        return this == COMPLETED || this == PARTIALLY_REFUNDED;
    }
}
