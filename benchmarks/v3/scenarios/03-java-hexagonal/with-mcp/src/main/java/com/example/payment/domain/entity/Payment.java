package com.example.payment.domain.entity;

import com.example.payment.domain.exception.InvalidPaymentStateException;
import com.example.payment.domain.valueobject.Money;
import com.example.payment.domain.valueobject.PaymentId;
import com.example.payment.domain.valueobject.PaymentStatus;

import java.time.Instant;

public class Payment {
    private final PaymentId id;
    private final String orderId;
    private final Money amount;
    private PaymentStatus status;
    private String gatewayTransactionId;
    private final Instant createdAt;
    private Instant updatedAt;

    public Payment(PaymentId id, String orderId, Money amount) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void markProcessing() {
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void complete(String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.gatewayTransactionId = transactionId;
        this.updatedAt = Instant.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void refund() {
        if (!status.canRefund()) {
            throw new InvalidPaymentStateException("Cannot refund payment in " + status + " state");
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public PaymentId getId() { return id; }
    public String getOrderId() { return orderId; }
    public Money getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
