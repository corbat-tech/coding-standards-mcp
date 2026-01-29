package com.example.payment.domain.entity;

import com.example.payment.domain.exception.InvalidPaymentOperationException;
import com.example.payment.domain.valueobject.Money;
import com.example.payment.domain.valueobject.PaymentId;
import com.example.payment.domain.valueobject.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payment domain entity - the aggregate root for payment operations.
 * Contains business logic for payment processing.
 */
public class Payment {

    private final PaymentId id;
    private final String orderId;
    private final String customerId;
    private final Money amount;
    private PaymentStatus status;
    private Money refundedAmount;
    private String gatewayTransactionId;
    private String failureReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Payment(Builder builder) {
        this.id = builder.id;
        this.orderId = builder.orderId;
        this.customerId = builder.customerId;
        this.amount = builder.amount;
        this.status = builder.status;
        this.refundedAmount = builder.refundedAmount;
        this.gatewayTransactionId = builder.gatewayTransactionId;
        this.failureReason = builder.failureReason;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    /**
     * Factory method to create a new payment.
     */
    public static Payment create(String orderId, String customerId, Money amount) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");

        if (amount.isZero()) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        return builder()
                .id(PaymentId.generate())
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .refundedAmount(Money.zero(amount.getCurrencyCode()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Marks the payment as processing.
     */
    public void startProcessing() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentOperationException(
                    "Cannot start processing payment with status: " + status);
        }
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks the payment as completed.
     */
    public void complete(String gatewayTransactionId) {
        if (status != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentOperationException(
                    "Cannot complete payment with status: " + status);
        }
        Objects.requireNonNull(gatewayTransactionId, "Gateway transaction ID cannot be null");

        this.status = PaymentStatus.COMPLETED;
        this.gatewayTransactionId = gatewayTransactionId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks the payment as failed.
     */
    public void fail(String reason) {
        if (status != PaymentStatus.PROCESSING && status != PaymentStatus.PENDING) {
            throw new InvalidPaymentOperationException(
                    "Cannot fail payment with status: " + status);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Initiates a refund for this payment.
     */
    public void initiateRefund(Money refundAmount) {
        Objects.requireNonNull(refundAmount, "Refund amount cannot be null");

        if (!status.canBeRefunded()) {
            throw new InvalidPaymentOperationException(
                    "Cannot refund payment with status: " + status);
        }

        Money totalRefunded = this.refundedAmount.add(refundAmount);
        if (totalRefunded.isGreaterThan(this.amount)) {
            throw new InvalidPaymentOperationException(
                    "Refund amount exceeds remaining refundable amount");
        }

        this.status = PaymentStatus.REFUND_PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Completes the refund process.
     */
    public void completeRefund(Money refundAmount) {
        if (status != PaymentStatus.REFUND_PENDING) {
            throw new InvalidPaymentOperationException(
                    "Cannot complete refund for payment with status: " + status);
        }

        this.refundedAmount = this.refundedAmount.add(refundAmount);

        if (this.refundedAmount.equals(this.amount)) {
            this.status = PaymentStatus.REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the amount that can still be refunded.
     */
    public Money getRefundableAmount() {
        return this.amount.subtract(this.refundedAmount);
    }

    /**
     * Checks if the payment can be refunded for the given amount.
     */
    public boolean canRefund(Money refundAmount) {
        if (!status.canBeRefunded()) {
            return false;
        }
        return refundAmount.isGreaterThanOrEqual(Money.zero(amount.getCurrencyCode())) &&
               getRefundableAmount().isGreaterThanOrEqual(refundAmount);
    }

    // Getters
    public PaymentId getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Money getRefundedAmount() {
        return refundedAmount;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PaymentId id;
        private String orderId;
        private String customerId;
        private Money amount;
        private PaymentStatus status;
        private Money refundedAmount;
        private String gatewayTransactionId;
        private String failureReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(PaymentId id) {
            this.id = id;
            return this;
        }

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public Builder refundedAmount(Money refundedAmount) {
            this.refundedAmount = refundedAmount;
            return this;
        }

        public Builder gatewayTransactionId(String gatewayTransactionId) {
            this.gatewayTransactionId = gatewayTransactionId;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Payment build() {
            return new Payment(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", amount=" + amount +
                ", status=" + status +
                '}';
    }
}
