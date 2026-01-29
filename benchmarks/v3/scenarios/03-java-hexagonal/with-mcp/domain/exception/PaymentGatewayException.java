package com.payment.domain.exception;

/**
 * Thrown when payment gateway operations fail.
 */
public class PaymentGatewayException extends PaymentException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
