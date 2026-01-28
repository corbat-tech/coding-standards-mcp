package com.example.order.infrastructure.kafka;

import com.example.order.domain.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaDeadLetterQueuePublisher implements DeadLetterQueuePublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaDeadLetterQueuePublisher.class);

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final String dlqTopic;

    public KafkaDeadLetterQueuePublisher(
        KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
        @Value("${kafka.topics.order-created-dlq}") String dlqTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.dlqTopic = dlqTopic;
    }

    @Override
    public void publish(OrderCreatedEvent event, String reason) {
        log.warn("Publishing to DLQ: {} - reason: {}", event.orderId(), reason);
        kafkaTemplate.send(dlqTopic, event.orderId(), event);
    }
}
