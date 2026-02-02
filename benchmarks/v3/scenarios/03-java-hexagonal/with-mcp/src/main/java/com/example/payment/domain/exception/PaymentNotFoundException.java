package com.example.payment.domain.exception;

public class PaymentNotFoundException extends PaymentException {
    public PaymentNotFoundException(String paymentId) {
        super("Payment not found: " + paymentId);
    }
}
