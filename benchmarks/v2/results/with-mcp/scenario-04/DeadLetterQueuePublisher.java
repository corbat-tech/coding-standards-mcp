package com.example.order.infrastructure.kafka;

import com.example.order.domain.event.OrderCreatedEvent;

public interface DeadLetterQueuePublisher {
    void publish(OrderCreatedEvent event, String reason);
}
