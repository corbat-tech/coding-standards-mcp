package com.example.payment.domain.valueobject;

/**
 * Enum representing the possible states of a payment.
 */
public enum PaymentStatus {

    PENDING("Payment is pending processing"),
    PROCESSING("Payment is being processed"),
    COMPLETED("Payment completed successfully"),
    FAILED("Payment processing failed"),
    REFUND_PENDING("Refund is pending"),
    REFUNDED("Payment has been refunded"),
    PARTIALLY_REFUNDED("Payment has been partially refunded"),
    CANCELLED("Payment was cancelled");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED ||
               this == REFUNDED || this == CANCELLED;
    }

    public boolean canBeRefunded() {
        return this == COMPLETED || this == PARTIALLY_REFUNDED;
    }

    public boolean isSuccessful() {
        return this == COMPLETED || this == PARTIALLY_REFUNDED;
    }
}
