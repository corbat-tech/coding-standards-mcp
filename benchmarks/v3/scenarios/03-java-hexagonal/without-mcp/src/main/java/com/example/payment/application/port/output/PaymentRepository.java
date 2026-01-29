package com.example.payment.application.port.output;

import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.valueobject.PaymentId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for payment persistence operations.
 * Defines the contract for payment data storage.
 */
public interface PaymentRepository {

    /**
     * Save a payment.
     *
     * @param payment the payment to save
     * @return the saved payment
     */
    Payment save(Payment payment);

    /**
     * Find a payment by ID.
     *
     * @param paymentId the payment ID
     * @return optional containing the payment if found
     */
    Optional<Payment> findById(PaymentId paymentId);

    /**
     * Find all payments for a customer.
     *
     * @param customerId the customer ID
     * @return list of payments
     */
    List<Payment> findByCustomerId(String customerId);

    /**
     * Find all payments for an order.
     *
     * @param orderId the order ID
     * @return list of payments
     */
    List<Payment> findByOrderId(String orderId);

    /**
     * Check if a payment exists.
     *
     * @param paymentId the payment ID
     * @return true if payment exists
     */
    boolean existsById(PaymentId paymentId);

    /**
     * Delete a payment.
     *
     * @param paymentId the payment ID
     */
    void deleteById(PaymentId paymentId);
}
