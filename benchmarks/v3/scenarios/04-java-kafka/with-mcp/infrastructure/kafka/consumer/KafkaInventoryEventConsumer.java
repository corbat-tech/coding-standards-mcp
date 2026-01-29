package com.example.order.infrastructure.kafka.consumer;

import com.example.order.application.port.in.ProcessOrderEventUseCase;
import com.example.order.application.port.in.ProcessOrderEventUseCase.ProcessingResult;
import com.example.order.domain.events.OrderCreatedEvent;
import com.example.order.infrastructure.kafka.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer adapter for processing order events.
 * Delegates to the ProcessOrderEventUseCase for business logic.
 * Failed messages are automatically sent to DLQ by error handler.
 */
@Component
public class KafkaInventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaInventoryEventConsumer.class);

    private final ProcessOrderEventUseCase processOrderEventUseCase;

    public KafkaInventoryEventConsumer(ProcessOrderEventUseCase processOrderEventUseCase) {
        this.processOrderEventUseCase = processOrderEventUseCase;
    }

    @KafkaListener(
        topics = KafkaConfig.ORDER_EVENTS_TOPIC,
        groupId = "${spring.kafka.consumer.group-id:inventory-service}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received OrderCreatedEvent: eventId={}, partition={}, offset={}",
            event.eventId(), partition, offset);

        ProcessingResult result = processOrderEventUseCase.processOrderCreated(event);

        logProcessingResult(result, partition, offset);

        if (!result.success() && !result.skipped()) {
            // Throwing exception triggers DLQ via error handler
            throw new RuntimeException("Event processing failed: " + result.message());
        }
    }

    private void logProcessingResult(ProcessingResult result, int partition, long offset) {
        if (result.skipped()) {
            log.info("Event skipped (duplicate): eventId={}, partition={}, offset={}",
                result.eventId(), partition, offset);
        } else if (result.success()) {
            log.info("Event processed successfully: eventId={}, partition={}, offset={}",
                result.eventId(), partition, offset);
        } else {
            log.warn("Event processing failed: eventId={}, message={}, partition={}, offset={}",
                result.eventId(), result.message(), partition, offset);
        }
    }
}
