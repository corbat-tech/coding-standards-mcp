package com.example.order.application.port.out;

import com.example.order.domain.events.OrderCreatedEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Output port for publishing order events.
 * Implemented by infrastructure adapters (e.g., KafkaOrderEventPublisher).
 */
public interface OrderEventPublisher {

    /**
     * Publishes an OrderCreatedEvent asynchronously.
     *
     * @param event the event to publish
     * @return a CompletableFuture that completes when the event is acknowledged
     */
    CompletableFuture<Void> publishOrderCreated(OrderCreatedEvent event);

    /**
     * Publishes an OrderCreatedEvent synchronously (blocking).
     *
     * @param event the event to publish
     * @throws com.example.order.domain.exception.EventProcessingException if publishing fails
     */
    void publishOrderCreatedSync(OrderCreatedEvent event);
}
