package com.payment.domain.port.output;

import com.payment.domain.valueobject.Money;

/**
 * Output port for payment gateway operations.
 * Defines the contract that external payment providers must implement.
 */
public interface PaymentGateway {

    /**
     * Process a charge through the payment gateway.
     *
     * @param request the charge request details
     * @return the charge result from the gateway
     */
    ChargeResult charge(ChargeRequest request);

    /**
     * Process a refund through the payment gateway.
     *
     * @param request the refund request details
     * @return the refund result from the gateway
     */
    RefundResult refund(RefundRequest request);

    /**
     * Request to charge a payment method.
     */
    record ChargeRequest(
        String customerId,
        String orderId,
        Money amount,
        String paymentMethod,
        String idempotencyKey
    ) {}

    /**
     * Result of a charge operation.
     */
    record ChargeResult(
        boolean success,
        String transactionId,
        String errorCode,
        String errorMessage
    ) {
        public static ChargeResult success(String transactionId) {
            return new ChargeResult(true, transactionId, null, null);
        }

        public static ChargeResult failure(String errorCode, String message) {
            return new ChargeResult(false, null, errorCode, message);
        }
    }

    /**
     * Request to refund a transaction.
     */
    record RefundRequest(
        String originalTransactionId,
        Money amount,
        String reason
    ) {}

    /**
     * Result of a refund operation.
     */
    record RefundResult(
        boolean success,
        String refundTransactionId,
        String errorCode,
        String errorMessage
    ) {
        public static RefundResult success(String refundTransactionId) {
            return new RefundResult(true, refundTransactionId, null, null);
        }

        public static RefundResult failure(String errorCode, String message) {
            return new RefundResult(false, null, errorCode, message);
        }
    }
}
