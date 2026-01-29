package com.example.payment.domain.exception;

import com.example.payment.domain.valueobject.PaymentId;

/**
 * Exception thrown when a payment is not found.
 */
public class PaymentNotFoundException extends RuntimeException {

    private final PaymentId paymentId;

    public PaymentNotFoundException(PaymentId paymentId) {
        super("Payment not found with ID: " + paymentId);
        this.paymentId = paymentId;
    }

    public PaymentNotFoundException(String paymentId) {
        super("Payment not found with ID: " + paymentId);
        this.paymentId = PaymentId.of(paymentId);
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }
}
