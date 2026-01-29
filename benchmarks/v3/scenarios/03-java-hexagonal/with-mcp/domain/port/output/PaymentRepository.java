package com.payment.domain.port.output;

import com.payment.domain.entity.Payment;
import com.payment.domain.valueobject.PaymentId;

import java.util.Optional;

/**
 * Output port for payment persistence.
 * Defines the contract that storage implementations must follow.
 */
public interface PaymentRepository {

    /**
     * Save a payment to the repository.
     *
     * @param payment the payment to save
     * @return the saved payment
     */
    Payment save(Payment payment);

    /**
     * Find a payment by its ID.
     *
     * @param id the payment identifier
     * @return the payment if found
     */
    Optional<Payment> findById(PaymentId id);

    /**
     * Find a payment by order ID.
     *
     * @param orderId the order identifier
     * @return the payment if found
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Check if a payment exists for the given order.
     *
     * @param orderId the order identifier
     * @return true if a payment exists
     */
    boolean existsByOrderId(String orderId);
}
