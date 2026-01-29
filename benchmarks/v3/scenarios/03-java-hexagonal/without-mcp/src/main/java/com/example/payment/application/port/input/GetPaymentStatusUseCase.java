package com.example.payment.application.port.input;

import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.valueobject.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Input port for querying payment status.
 * Defines the contract for payment status retrieval use case.
 */
public interface GetPaymentStatusUseCase {

    /**
     * Get payment by ID.
     *
     * @param paymentId the payment ID
     * @return the payment status response
     */
    PaymentStatusResponse getPaymentStatus(String paymentId);

    /**
     * Get all payments for a customer.
     *
     * @param customerId the customer ID
     * @return list of payment status responses
     */
    List<PaymentStatusResponse> getPaymentsByCustomer(String customerId);

    /**
     * Get all payments for an order.
     *
     * @param orderId the order ID
     * @return list of payment status responses
     */
    List<PaymentStatusResponse> getPaymentsByOrder(String orderId);

    /**
     * Response object for payment status.
     */
    record PaymentStatusResponse(
            String paymentId,
            String orderId,
            String customerId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String statusDescription,
            BigDecimal refundedAmount,
            String gatewayTransactionId,
            String failureReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static PaymentStatusResponse fromPayment(Payment payment) {
            return new PaymentStatusResponse(
                    payment.getId().toString(),
                    payment.getOrderId(),
                    payment.getCustomerId(),
                    payment.getAmount().getAmount(),
                    payment.getAmount().getCurrencyCode(),
                    payment.getStatus(),
                    payment.getStatus().getDescription(),
                    payment.getRefundedAmount().getAmount(),
                    payment.getGatewayTransactionId(),
                    payment.getFailureReason(),
                    payment.getCreatedAt(),
                    payment.getUpdatedAt()
            );
        }
    }
}
