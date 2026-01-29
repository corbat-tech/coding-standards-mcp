package com.payment.domain.port.input;

import com.payment.domain.valueobject.Money;
import com.payment.domain.valueobject.PaymentId;

/**
 * Input port for refunding payments.
 * Defines the contract for the primary/driving adapter.
 */
public interface RefundPaymentUseCase {

    /**
     * Refund a payment (fully or partially).
     *
     * @param command the refund command
     * @return the result of the refund operation
     */
    RefundResult execute(RefundCommand command);

    /**
     * Command object for refund processing.
     */
    record RefundCommand(
        PaymentId paymentId,
        Money amount,
        String reason
    ) {
        public RefundCommand {
            if (paymentId == null) {
                throw new IllegalArgumentException("Payment ID is required");
            }
            if (amount == null || !amount.isPositive()) {
                throw new IllegalArgumentException("Valid positive refund amount is required");
            }
        }
    }

    /**
     * Result of refund operation.
     */
    record RefundResult(
        PaymentId paymentId,
        boolean success,
        String status,
        Money refundedAmount,
        String message
    ) {
        public static RefundResult success(PaymentId id, Money amount) {
            return new RefundResult(id, true, "REFUNDED", amount, "Refund processed successfully");
        }

        public static RefundResult partialSuccess(PaymentId id, Money amount) {
            return new RefundResult(id, true, "PARTIALLY_REFUNDED", amount, "Partial refund processed");
        }

        public static RefundResult failed(PaymentId id, String reason) {
            return new RefundResult(id, false, "FAILED", null, reason);
        }
    }
}
