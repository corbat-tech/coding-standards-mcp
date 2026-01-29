package com.payment.domain.exception;

/**
 * Thrown when a payment operation is invalid for the current state.
 */
public class InvalidPaymentStateException extends PaymentException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
