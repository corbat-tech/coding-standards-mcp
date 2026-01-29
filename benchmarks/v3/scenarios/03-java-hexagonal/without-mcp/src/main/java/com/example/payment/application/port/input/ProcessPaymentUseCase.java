package com.example.payment.application.port.input;

import com.example.payment.domain.entity.Payment;

import java.math.BigDecimal;

/**
 * Input port for processing payments.
 * Defines the contract for payment processing use case.
 */
public interface ProcessPaymentUseCase {

    /**
     * Process a new payment.
     *
     * @param command the payment processing command
     * @return the processed payment
     */
    Payment processPayment(ProcessPaymentCommand command);

    /**
     * Command object for processing a payment.
     */
    record ProcessPaymentCommand(
            String orderId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String customerEmail
    ) {
        public ProcessPaymentCommand {
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("Order ID is required");
            }
            if (customerId == null || customerId.isBlank()) {
                throw new IllegalArgumentException("Customer ID is required");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero");
            }
            if (currency == null || currency.isBlank()) {
                throw new IllegalArgumentException("Currency is required");
            }
            if (paymentMethod == null || paymentMethod.isBlank()) {
                throw new IllegalArgumentException("Payment method is required");
            }
        }
    }
}
