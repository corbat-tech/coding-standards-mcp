package com.example.payment.domain.exception;

public class InvalidPaymentStateException extends PaymentException {
    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
