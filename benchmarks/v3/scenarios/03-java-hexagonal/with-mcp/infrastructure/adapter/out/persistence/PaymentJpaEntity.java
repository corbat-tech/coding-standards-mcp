package com.payment.infrastructure.adapter.out.persistence;

import com.payment.domain.entity.Payment;
import com.payment.domain.entity.PaymentStatus;
import com.payment.domain.valueobject.Money;
import com.payment.domain.valueobject.PaymentId;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

/**
 * JPA entity for Payment persistence.
 * Maps domain entity to database schema.
 */
@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "refunded_amount", precision = 19, scale = 2)
    private BigDecimal refundedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentJpaEntity() {}

    public static PaymentJpaEntity fromDomain(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.id = payment.getId().getValue();
        entity.orderId = payment.getOrderId();
        entity.customerId = payment.getCustomerId();
        entity.amount = payment.getAmount().getAmount();
        entity.currencyCode = payment.getAmount().getCurrency().getCurrencyCode();
        entity.refundedAmount = payment.getRefundedAmount().getAmount();
        entity.status = payment.getStatus();
        entity.gatewayTransactionId = payment.getGatewayTransactionId();
        entity.createdAt = payment.getCreatedAt();
        entity.updatedAt = payment.getUpdatedAt();
        return entity;
    }

    public Payment toDomain() {
        return Payment.reconstitute(
            PaymentId.of(id),
            orderId,
            customerId,
            Money.of(amount, Currency.getInstance(currencyCode)),
            Money.of(refundedAmount, Currency.getInstance(currencyCode)),
            status,
            gatewayTransactionId,
            createdAt,
            updatedAt
        );
    }

    // Getters for JPA
    public UUID getId() { return id; }
    public String getOrderId() { return orderId; }
}
