package com.payment.domain.exception;

/**
 * Base exception for all payment-related domain errors.
 */
public abstract class PaymentException extends RuntimeException {

    protected PaymentException(String message) {
        super(message);
    }

    protected PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
