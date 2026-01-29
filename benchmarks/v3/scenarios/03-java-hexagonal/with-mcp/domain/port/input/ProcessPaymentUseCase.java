package com.payment.domain.port.input;

import com.payment.domain.valueobject.Money;
import com.payment.domain.valueobject.PaymentId;

/**
 * Input port for processing payments.
 * Defines the contract for the primary/driving adapter.
 */
public interface ProcessPaymentUseCase {

    /**
     * Process a new payment.
     *
     * @param command the payment processing command
     * @return the result of payment processing
     */
    ProcessPaymentResult execute(ProcessPaymentCommand command);

    /**
     * Command object for payment processing.
     */
    record ProcessPaymentCommand(
        String orderId,
        String customerId,
        Money amount,
        String paymentMethod
    ) {
        public ProcessPaymentCommand {
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("Order ID is required");
            }
            if (customerId == null || customerId.isBlank()) {
                throw new IllegalArgumentException("Customer ID is required");
            }
            if (amount == null || !amount.isPositive()) {
                throw new IllegalArgumentException("Valid positive amount is required");
            }
        }
    }

    /**
     * Result of payment processing.
     */
    record ProcessPaymentResult(
        PaymentId paymentId,
        String status,
        String transactionId,
        String message
    ) {
        public static ProcessPaymentResult success(PaymentId id, String transactionId) {
            return new ProcessPaymentResult(id, "COMPLETED", transactionId, "Payment processed successfully");
        }

        public static ProcessPaymentResult failed(PaymentId id, String reason) {
            return new ProcessPaymentResult(id, "FAILED", null, reason);
        }
    }
}
