package com.payment.domain.entity;

import com.payment.domain.exception.InvalidPaymentStateException;
import com.payment.domain.valueobject.Money;
import com.payment.domain.valueobject.PaymentId;

import java.time.Instant;
import java.util.Objects;

/**
 * Payment aggregate root.
 * Encapsulates business rules and ensures invariants.
 */
public class Payment {

    private final PaymentId id;
    private final String orderId;
    private final String customerId;
    private final Money amount;
    private Money refundedAmount;
    private PaymentStatus status;
    private String gatewayTransactionId;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(PaymentId id, String orderId, String customerId, Money amount) {
        this.id = Objects.requireNonNull(id, "Payment ID cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.refundedAmount = Money.zero(amount.getCurrency());
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static Payment create(String orderId, String customerId, Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        return new Payment(PaymentId.generate(), orderId, customerId, amount);
    }

    public static Payment reconstitute(
            PaymentId id, String orderId, String customerId, Money amount,
            Money refundedAmount, PaymentStatus status, String gatewayTransactionId,
            Instant createdAt, Instant updatedAt) {
        Payment payment = new Payment(id, orderId, customerId, amount);
        payment.refundedAmount = refundedAmount;
        payment.status = status;
        payment.gatewayTransactionId = gatewayTransactionId;
        payment.updatedAt = updatedAt;
        return payment;
    }

    public void markAsProcessing() {
        transitionTo(PaymentStatus.PROCESSING);
    }

    public void markAsCompleted(String transactionId) {
        Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        transitionTo(PaymentStatus.COMPLETED);
        this.gatewayTransactionId = transactionId;
    }

    public void markAsFailed() {
        transitionTo(PaymentStatus.FAILED);
    }

    public void refund(Money refundAmount) {
        if (!status.isRefundable()) {
            throw new InvalidPaymentStateException(
                "Cannot refund payment in status: " + status
            );
        }
        Money totalRefund = refundedAmount.add(refundAmount);
        if (totalRefund.isGreaterThan(amount)) {
            throw new IllegalArgumentException("Refund amount exceeds original payment");
        }
        this.refundedAmount = totalRefund;
        this.status = totalRefund.equals(amount)
            ? PaymentStatus.REFUNDED
            : PaymentStatus.PARTIALLY_REFUNDED;
        this.updatedAt = Instant.now();
    }

    private void transitionTo(PaymentStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidPaymentStateException(
                "Cannot transition from " + status + " to " + newStatus
            );
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    // Getters
    public PaymentId getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public Money getAmount() { return amount; }
    public Money getRefundedAmount() { return refundedAmount; }
    public PaymentStatus getStatus() { return status; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Money getRemainingRefundable() {
        return amount.subtract(refundedAmount);
    }
}
