package com.example.payment.adapter.input.rest.dto;

import com.example.payment.application.port.input.GetPaymentStatusUseCase.PaymentStatusResponse;
import com.example.payment.domain.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for payment responses.
 */
public record PaymentResponse(
        String paymentId,
        String orderId,
        String customerId,
        BigDecimal amount,
        String currency,
        String status,
        String statusDescription,
        BigDecimal refundedAmount,
        String gatewayTransactionId,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponse fromDomain(Payment payment) {
        return new PaymentResponse(
                payment.getId().toString(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount().getAmount(),
                payment.getAmount().getCurrencyCode(),
                payment.getStatus().name(),
                payment.getStatus().getDescription(),
                payment.getRefundedAmount().getAmount(),
                payment.getGatewayTransactionId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    public static PaymentResponse fromStatusResponse(PaymentStatusResponse response) {
        return new PaymentResponse(
                response.paymentId(),
                response.orderId(),
                response.customerId(),
                response.amount(),
                response.currency(),
                response.status().name(),
                response.statusDescription(),
                response.refundedAmount(),
                response.gatewayTransactionId(),
                response.failureReason(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
