package com.example.orderprocessing.consumer;

import com.example.orderprocessing.config.KafkaConfig;
import com.example.orderprocessing.domain.event.OrderCreatedEvent;
import com.example.orderprocessing.exception.InsufficientStockException;
import com.example.orderprocessing.exception.ProductNotFoundException;
import com.example.orderprocessing.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for processing order events and updating inventory.
 * Implements idempotent message processing and handles failures gracefully.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final InventoryService inventoryService;

    /**
     * Consumes OrderCreatedEvent messages and updates inventory accordingly.
     * Uses manual acknowledgment for reliable processing.
     *
     * @param record The Kafka consumer record containing the event
     * @param acknowledgment The acknowledgment handle for manual commit
     */
    @KafkaListener(
            topics = KafkaConfig.ORDER_CREATED_TOPIC,
            groupId = KafkaConfig.INVENTORY_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCreatedEvent(
            ConsumerRecord<String, OrderCreatedEvent> record,
            Acknowledgment acknowledgment) {

        OrderCreatedEvent event = record.value();

        log.info("Received OrderCreatedEvent: eventId={}, orderId={}, partition={}, offset={}",
                event.getEventId(), event.getOrderId(),
                record.partition(), record.offset());

        try {
            boolean processed = inventoryService.processOrderCreatedEvent(event);

            if (processed) {
                log.info("Successfully processed event: eventId={}", event.getEventId());
            } else {
                log.info("Event was duplicate, already processed: eventId={}", event.getEventId());
            }

            // Acknowledge the message
            acknowledgment.acknowledge();
            log.debug("Message acknowledged: eventId={}", event.getEventId());

        } catch (ProductNotFoundException e) {
            log.error("Product not found while processing event: eventId={}, productId={}",
                    event.getEventId(), e.getProductId());
            // This is a business error - acknowledge to prevent infinite retry
            // The message will be sent to DLT by error handler before this point
            // if we let the exception propagate
            throw e;

        } catch (InsufficientStockException e) {
            log.error("Insufficient stock while processing event: eventId={}, productId={}, requested={}, available={}",
                    event.getEventId(), e.getProductId(),
                    e.getRequestedQuantity(), e.getAvailableQuantity());
            // Let the error handler manage retries and DLT
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error processing event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            // Let the error handler manage retries and DLT
            throw e;
        }
    }

    /**
     * Consumes messages from the Dead Letter Topic for monitoring/alerting.
     */
    @KafkaListener(
            topics = KafkaConfig.ORDER_CREATED_DLT,
            groupId = KafkaConfig.INVENTORY_CONSUMER_GROUP + "-dlt"
    )
    public void consumeDeadLetterMessages(
            ConsumerRecord<String, OrderCreatedEvent> record,
            Acknowledgment acknowledgment) {

        OrderCreatedEvent event = record.value();

        log.error("Received message in DLT: eventId={}, orderId={}, partition={}, offset={}",
                event != null ? event.getEventId() : "null",
                event != null ? event.getOrderId() : "null",
                record.partition(), record.offset());

        // Log headers for debugging
        record.headers().forEach(header ->
                log.error("DLT message header: {}={}",
                        header.key(), new String(header.value())));

        // Here you could:
        // 1. Send alerts to operations team
        // 2. Store in a separate database for manual review
        // 3. Attempt manual remediation

        acknowledgment.acknowledge();
    }
}
