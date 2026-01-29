package com.example.saga.application.port;

import java.math.BigDecimal;

/**
 * Port interface for payment processing operations.
 */
public interface PaymentService {

    /**
     * Processes a payment for an order.
     *
     * @param orderId    the order ID
     * @param customerId the customer ID
     * @param amount     the payment amount
     * @return the transaction ID
     */
    String processPayment(String orderId, String customerId, BigDecimal amount);

    /**
     * Refunds a previously processed payment.
     *
     * @param transactionId the transaction ID to refund
     */
    void refundPayment(String transactionId);
}
