package com.example.saga.application.saga;

import com.example.saga.domain.entity.Order;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable context shared across saga steps.
 * Contains the state accumulated during saga execution.
 */
public final class SagaContext {

    private final String orderId;
    private final Order order;
    private final String inventoryReservationId;
    private final String paymentTransactionId;
    private final String shipmentTrackingId;
    private final Map<String, Object> metadata;

    private SagaContext(Builder builder) {
        this.orderId = Objects.requireNonNull(builder.orderId, "Order ID cannot be null");
        this.order = builder.order;
        this.inventoryReservationId = builder.inventoryReservationId;
        this.paymentTransactionId = builder.paymentTransactionId;
        this.shipmentTrackingId = builder.shipmentTrackingId;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    /**
     * Creates a new saga context with the given order ID.
     */
    public static SagaContext create(String orderId) {
        return new Builder(orderId).build();
    }

    public String getOrderId() {
        return orderId;
    }

    public Optional<Order> getOrder() {
        return Optional.ofNullable(order);
    }

    public Optional<String> getInventoryReservationId() {
        return Optional.ofNullable(inventoryReservationId);
    }

    public Optional<String> getPaymentTransactionId() {
        return Optional.ofNullable(paymentTransactionId);
    }

    public Optional<String> getShipmentTrackingId() {
        return Optional.ofNullable(shipmentTrackingId);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Creates a new context with the order set.
     */
    public SagaContext withOrder(Order order) {
        return toBuilder().order(order).build();
    }

    /**
     * Creates a new context with the inventory reservation ID set.
     */
    public SagaContext withInventoryReservation(String reservationId) {
        return toBuilder().inventoryReservationId(reservationId).build();
    }

    /**
     * Creates a new context with the payment transaction ID set.
     */
    public SagaContext withPaymentTransaction(String transactionId) {
        return toBuilder().paymentTransactionId(transactionId).build();
    }

    /**
     * Creates a new context with the shipment tracking ID set.
     */
    public SagaContext withShipmentTracking(String trackingId) {
        return toBuilder().shipmentTrackingId(trackingId).build();
    }

    /**
     * Creates a new context with additional metadata.
     */
    public SagaContext withMetadata(String key, Object value) {
        return toBuilder().addMetadata(key, value).build();
    }

    private Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return "SagaContext{" +
               "orderId='" + orderId + '\'' +
               ", order=" + order +
               ", inventoryReservationId='" + inventoryReservationId + '\'' +
               ", paymentTransactionId='" + paymentTransactionId + '\'' +
               ", shipmentTrackingId='" + shipmentTrackingId + '\'' +
               ", metadata=" + metadata +
               '}';
    }

    /**
     * Builder for creating SagaContext instances.
     */
    public static class Builder {
        private final String orderId;
        private Order order;
        private String inventoryReservationId;
        private String paymentTransactionId;
        private String shipmentTrackingId;
        private final Map<String, Object> metadata = new HashMap<>();

        public Builder(String orderId) {
            this.orderId = orderId;
        }

        private Builder(SagaContext context) {
            this.orderId = context.orderId;
            this.order = context.order;
            this.inventoryReservationId = context.inventoryReservationId;
            this.paymentTransactionId = context.paymentTransactionId;
            this.shipmentTrackingId = context.shipmentTrackingId;
            this.metadata.putAll(context.metadata);
        }

        public Builder order(Order order) {
            this.order = order;
            return this;
        }

        public Builder inventoryReservationId(String reservationId) {
            this.inventoryReservationId = reservationId;
            return this;
        }

        public Builder paymentTransactionId(String transactionId) {
            this.paymentTransactionId = transactionId;
            return this;
        }

        public Builder shipmentTrackingId(String trackingId) {
            this.shipmentTrackingId = trackingId;
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public SagaContext build() {
            return new SagaContext(this);
        }
    }
}
