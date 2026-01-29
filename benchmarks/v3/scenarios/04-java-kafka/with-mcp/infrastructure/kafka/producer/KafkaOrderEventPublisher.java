package com.example.order.infrastructure.kafka.producer;

import com.example.order.application.port.out.OrderEventPublisher;
import com.example.order.domain.events.OrderCreatedEvent;
import com.example.order.domain.exception.EventProcessingException;
import com.example.order.infrastructure.kafka.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka adapter for publishing order events.
 * Implements the OrderEventPublisher output port.
 */
@Component
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);
    private static final long SYNC_PUBLISH_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public KafkaOrderEventPublisher(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<Void> publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent asynchronously: eventId={}, orderId={}",
            event.eventId(), event.orderId());

        return kafkaTemplate.send(
                KafkaConfig.ORDER_EVENTS_TOPIC,
                event.orderId(),
                event
            )
            .thenAccept(result -> logSuccess(event, result))
            .exceptionally(ex -> {
                logFailure(event, ex);
                throw new EventProcessingException(
                    event.eventId(),
                    "Failed to publish event: " + ex.getMessage(),
                    ex,
                    true
                );
            });
    }

    @Override
    public void publishOrderCreatedSync(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent synchronously: eventId={}, orderId={}",
            event.eventId(), event.orderId());

        try {
            SendResult<String, OrderCreatedEvent> result = kafkaTemplate.send(
                    KafkaConfig.ORDER_EVENTS_TOPIC,
                    event.orderId(),
                    event
                )
                .get(SYNC_PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            logSuccess(event, result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw createPublishException(event, e);
        } catch (ExecutionException e) {
            throw createPublishException(event, e.getCause());
        } catch (TimeoutException e) {
            throw createPublishException(event, e);
        }
    }

    private void logSuccess(OrderCreatedEvent event, SendResult<String, OrderCreatedEvent> result) {
        log.info("Event published successfully: eventId={}, topic={}, partition={}, offset={}",
            event.eventId(),
            result.getRecordMetadata().topic(),
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset()
        );
    }

    private void logFailure(OrderCreatedEvent event, Throwable ex) {
        log.error("Failed to publish event: eventId={}", event.eventId(), ex);
    }

    private EventProcessingException createPublishException(
            OrderCreatedEvent event, Throwable cause) {
        return new EventProcessingException(
            event.eventId(),
            "Failed to publish event: " + cause.getMessage(),
            cause,
            true
        );
    }
}
