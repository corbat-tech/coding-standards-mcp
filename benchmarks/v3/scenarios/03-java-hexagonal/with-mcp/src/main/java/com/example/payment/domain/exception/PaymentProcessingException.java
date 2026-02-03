package com.example.payment.domain.exception;

public class PaymentProcessingException extends PaymentException {
    public PaymentProcessingException(String message) {
        super(message);
    }
}
