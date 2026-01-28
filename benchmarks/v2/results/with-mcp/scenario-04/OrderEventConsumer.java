package com.example.order.infrastructure.kafka;

import com.example.order.application.OrderProcessingService;
import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.exception.DuplicateOrderException;
import com.example.order.domain.exception.OrderProcessingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderProcessingService processingService;
    private final DeadLetterQueuePublisher dlqPublisher;

    public OrderEventConsumer(
        OrderProcessingService processingService,
        DeadLetterQueuePublisher dlqPublisher
    ) {
        this.processingService = processingService;
        this.dlqPublisher = dlqPublisher;
    }

    @KafkaListener(
        topics = "${kafka.topics.order-created}",
        groupId = "${kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
        ConsumerRecord<String, OrderCreatedEvent> record,
        Acknowledgment acknowledgment
    ) {
        OrderCreatedEvent event = record.value();
        log.info("Received order event: {}", event.orderId());

        try {
            processingService.process(event);
            acknowledgment.acknowledge();
        } catch (DuplicateOrderException e) {
            log.warn("Duplicate order ignored: {}", e.getOrderId());
            acknowledgment.acknowledge();
        } catch (OrderProcessingException e) {
            handleProcessingException(event, e, acknowledgment);
        } catch (Exception e) {
            handleUnexpectedException(event, e, acknowledgment);
        }
    }

    private void handleProcessingException(
        OrderCreatedEvent event,
        OrderProcessingException e,
        Acknowledgment acknowledgment
    ) {
        log.error("Order processing failed: {} - {}", e.getOrderId(), e.getMessage());

        if (e.isRetryable()) {
            throw e;
        }

        processingService.processWithFailure(event, e.getMessage());
        dlqPublisher.publish(event, e.getMessage());
        acknowledgment.acknowledge();
    }

    private void handleUnexpectedException(
        OrderCreatedEvent event,
        Exception e,
        Acknowledgment acknowledgment
    ) {
        log.error("Unexpected error processing order: {}", event.orderId(), e);
        processingService.processWithFailure(event, "Unexpected error: " + e.getMessage());
        dlqPublisher.publish(event, e.getMessage());
        acknowledgment.acknowledge();
    }
}
