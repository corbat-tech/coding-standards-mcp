package com.example.kafka.infrastructure.kafka;

import com.example.kafka.application.order.OrderEventPublisher;
import com.example.kafka.domain.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventKafkaPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventKafkaPublisher.class);

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final String topic;

    public OrderEventKafkaPublisher(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
                                     @Value("${app.kafka.topics.order-created}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        kafkaTemplate.send(topic, event.orderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event for order {}", event.orderId(), ex);
                    } else {
                        log.info("Published OrderCreatedEvent for order {} to partition {}",
                                event.orderId(), result.getRecordMetadata().partition());
                    }
                });
    }
}
