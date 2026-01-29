package com.example.payment.application.port.input;

import com.example.payment.domain.entity.Payment;

import java.math.BigDecimal;

/**
 * Input port for refunding payments.
 * Defines the contract for payment refund use case.
 */
public interface RefundPaymentUseCase {

    /**
     * Refund a payment.
     *
     * @param command the refund command
     * @return the refunded payment
     */
    Payment refundPayment(RefundPaymentCommand command);

    /**
     * Command object for refunding a payment.
     */
    record RefundPaymentCommand(
            String paymentId,
            BigDecimal amount,
            String reason,
            String customerEmail
    ) {
        public RefundPaymentCommand {
            if (paymentId == null || paymentId.isBlank()) {
                throw new IllegalArgumentException("Payment ID is required");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Refund amount must be greater than zero");
            }
        }
    }
}
