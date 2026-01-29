package com.example.orderprocessing.producer;

import com.example.orderprocessing.config.KafkaConfig;
import com.example.orderprocessing.domain.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer for publishing order-related events to Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    /**
     * Publish an OrderCreatedEvent to Kafka.
     * Uses the orderId as the message key for partition consistency.
     *
     * @param event The event to publish
     * @return CompletableFuture containing the send result
     */
    public CompletableFuture<SendResult<String, OrderCreatedEvent>> publishOrderCreatedEvent(
            OrderCreatedEvent event) {

        log.info("Publishing OrderCreatedEvent: eventId={}, orderId={}",
                event.getEventId(), event.getOrderId());

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future =
                kafkaTemplate.send(
                        KafkaConfig.ORDER_CREATED_TOPIC,
                        event.getOrderId(),  // Key for partition assignment
                        event
                );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("OrderCreatedEvent published successfully: eventId={}, topic={}, partition={}, offset={}",
                        event.getEventId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish OrderCreatedEvent: eventId={}, error={}",
                        event.getEventId(), ex.getMessage(), ex);
            }
        });

        return future;
    }

    /**
     * Synchronously publish an OrderCreatedEvent (blocks until acknowledgment).
     *
     * @param event The event to publish
     * @return The send result
     */
    public SendResult<String, OrderCreatedEvent> publishOrderCreatedEventSync(OrderCreatedEvent event) {
        try {
            return publishOrderCreatedEvent(event).get();
        } catch (Exception e) {
            log.error("Error publishing OrderCreatedEvent synchronously: eventId={}",
                    event.getEventId(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
