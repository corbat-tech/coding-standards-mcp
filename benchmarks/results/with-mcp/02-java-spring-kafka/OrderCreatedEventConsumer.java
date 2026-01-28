package com.orders.infrastructure.messaging;

import com.orders.application.service.OrderProcessingService;
import com.orders.domain.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);

    private final OrderProcessingService orderProcessingService;

    public OrderCreatedEventConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        include = {Exception.class}
    )
    @KafkaListener(
        topics = "${kafka.topics.orders-created:orders.created}",
        groupId = "${spring.kafka.consumer.group-id:order-processor}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received order event: key={}, partition={}, offset={}, orderId={}",
            key, partition, offset, event.orderId());

        try {
            orderProcessingService.processOrder(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed order: {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to process order: {} - {}", event.orderId(), e.getMessage());
            throw e; // Will trigger retry mechanism
        }
    }

    @KafkaListener(
        topics = "${kafka.topics.orders-created:orders.created}-dlt",
        groupId = "${spring.kafka.consumer.group-id:order-processor}-dlt"
    )
    public void handleDlt(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {

        log.error("Received message in DLT: orderId={}, key={}, error={}",
            event.orderId(), key, exceptionMessage);

        // Store failed event for manual review/reprocessing
        handleDeadLetterEvent(event, exceptionMessage);
    }

    private void handleDeadLetterEvent(OrderCreatedEvent event, String errorMessage) {
        log.warn("Order {} moved to dead letter queue. Manual intervention required. Error: {}",
            event.orderId(), errorMessage);
        // In production: store in database, send alert, create ticket, etc.
    }
}
