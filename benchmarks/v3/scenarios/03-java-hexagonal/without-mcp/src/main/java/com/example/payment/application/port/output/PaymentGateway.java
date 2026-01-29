package com.example.payment.application.port.output;

import com.example.payment.domain.valueobject.Money;

/**
 * Output port for payment gateway operations.
 * Defines the contract for external payment processing.
 */
public interface PaymentGateway {

    /**
     * Process a payment through the gateway.
     *
     * @param request the payment request
     * @return the gateway response
     */
    PaymentGatewayResponse processPayment(PaymentGatewayRequest request);

    /**
     * Process a refund through the gateway.
     *
     * @param request the refund request
     * @return the gateway response
     */
    RefundGatewayResponse processRefund(RefundGatewayRequest request);

    /**
     * Request object for payment processing.
     */
    record PaymentGatewayRequest(
            String paymentId,
            Money amount,
            String paymentMethod,
            String customerId,
            String orderId
    ) {}

    /**
     * Response object from payment gateway.
     */
    record PaymentGatewayResponse(
            boolean success,
            String transactionId,
            String errorCode,
            String errorMessage
    ) {
        public static PaymentGatewayResponse success(String transactionId) {
            return new PaymentGatewayResponse(true, transactionId, null, null);
        }

        public static PaymentGatewayResponse failure(String errorCode, String errorMessage) {
            return new PaymentGatewayResponse(false, null, errorCode, errorMessage);
        }
    }

    /**
     * Request object for refund processing.
     */
    record RefundGatewayRequest(
            String originalTransactionId,
            Money amount,
            String reason
    ) {}

    /**
     * Response object from refund gateway.
     */
    record RefundGatewayResponse(
            boolean success,
            String refundTransactionId,
            String errorCode,
            String errorMessage
    ) {
        public static RefundGatewayResponse success(String refundTransactionId) {
            return new RefundGatewayResponse(true, refundTransactionId, null, null);
        }

        public static RefundGatewayResponse failure(String errorCode, String errorMessage) {
            return new RefundGatewayResponse(false, null, errorCode, errorMessage);
        }
    }
}
