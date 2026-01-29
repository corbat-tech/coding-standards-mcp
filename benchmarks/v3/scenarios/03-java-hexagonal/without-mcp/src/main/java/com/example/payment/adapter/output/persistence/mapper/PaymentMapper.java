package com.example.payment.adapter.output.persistence.mapper;

import com.example.payment.adapter.output.persistence.entity.PaymentJpaEntity;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.valueobject.Money;
import com.example.payment.domain.valueobject.PaymentId;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between domain Payment and JPA PaymentJpaEntity.
 */
@Component
public class PaymentMapper {

    /**
     * Convert domain entity to JPA entity.
     */
    public PaymentJpaEntity toJpaEntity(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(payment.getId().getValue());
        entity.setOrderId(payment.getOrderId());
        entity.setCustomerId(payment.getCustomerId());
        entity.setAmount(payment.getAmount().getAmount());
        entity.setCurrency(payment.getAmount().getCurrencyCode());
        entity.setStatus(payment.getStatus());
        entity.setRefundedAmount(payment.getRefundedAmount().getAmount());
        entity.setGatewayTransactionId(payment.getGatewayTransactionId());
        entity.setFailureReason(payment.getFailureReason());
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setUpdatedAt(payment.getUpdatedAt());
        return entity;
    }

    /**
     * Convert JPA entity to domain entity.
     */
    public Payment toDomain(PaymentJpaEntity entity) {
        return Payment.builder()
                .id(PaymentId.of(entity.getId()))
                .orderId(entity.getOrderId())
                .customerId(entity.getCustomerId())
                .amount(Money.of(entity.getAmount(), entity.getCurrency()))
                .status(entity.getStatus())
                .refundedAmount(Money.of(entity.getRefundedAmount(), entity.getCurrency()))
                .gatewayTransactionId(entity.getGatewayTransactionId())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
