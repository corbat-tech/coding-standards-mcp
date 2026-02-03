package com.example.kafka.infrastructure.kafka;

import com.example.kafka.domain.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterQueueHandler {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueHandler.class);

    @KafkaListener(
            topics = "${app.kafka.topics.order-created-dlt}",
            groupId = "dlt-group"
    )
    public void handleDeadLetter(OrderCreatedEvent event) {
        log.error("Received dead letter for order {}, eventId: {}", event.orderId(), event.eventId());
    }
}
