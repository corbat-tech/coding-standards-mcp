package com.example.kafka.application.order;

import com.example.kafka.domain.event.OrderCreatedEvent;

public interface OrderEventPublisher {
    void publish(OrderCreatedEvent event);
}
