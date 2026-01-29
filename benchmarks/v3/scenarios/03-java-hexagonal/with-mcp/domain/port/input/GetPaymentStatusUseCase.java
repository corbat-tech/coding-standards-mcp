package com.payment.domain.port.input;

import com.payment.domain.valueobject.Money;
import com.payment.domain.valueobject.PaymentId;

import java.time.Instant;

/**
 * Input port for querying payment status.
 * Defines the contract for the primary/driving adapter.
 */
public interface GetPaymentStatusUseCase {

    /**
     * Get the current status of a payment.
     *
     * @param paymentId the payment identifier
     * @return the payment status details
     */
    PaymentStatusResponse execute(PaymentId paymentId);

    /**
     * Response containing payment status details.
     */
    record PaymentStatusResponse(
        PaymentId paymentId,
        String orderId,
        String customerId,
        Money amount,
        Money refundedAmount,
        String status,
        String gatewayTransactionId,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
