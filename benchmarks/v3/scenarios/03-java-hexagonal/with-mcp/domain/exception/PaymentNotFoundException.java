package com.payment.domain.exception;

import com.payment.domain.valueobject.PaymentId;

/**
 * Thrown when a payment cannot be found.
 */
public class PaymentNotFoundException extends PaymentException {

    private final PaymentId paymentId;

    public PaymentNotFoundException(PaymentId paymentId) {
        super("Payment not found with ID: " + paymentId);
        this.paymentId = paymentId;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }
}
