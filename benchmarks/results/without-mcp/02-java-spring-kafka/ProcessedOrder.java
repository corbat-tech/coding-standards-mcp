package com.orders.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_orders")
public class ProcessedOrder {

    @Id
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant processedAt;

    private String errorMessage;

    public enum OrderStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public ProcessedOrder() {
    }

    public ProcessedOrder(String orderId, OrderStatus status, Instant processedAt) {
        this.orderId = orderId;
        this.status = status;
        this.processedAt = processedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
