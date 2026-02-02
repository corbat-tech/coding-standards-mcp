package com.example.payment.domain.valueobject;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED;

    public boolean canRefund() {
        return this == COMPLETED;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED;
    }
}
