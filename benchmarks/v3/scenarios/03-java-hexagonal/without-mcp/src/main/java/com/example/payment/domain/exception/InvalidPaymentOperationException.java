package com.example.payment.domain.exception;

/**
 * Exception thrown when an invalid operation is attempted on a payment.
 */
public class InvalidPaymentOperationException extends RuntimeException {

    public InvalidPaymentOperationException(String message) {
        super(message);
    }

    public InvalidPaymentOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
