package com.example.kafka.application.order;

import com.example.kafka.domain.event.OrderCreatedEvent;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public String createOrder(String orderId, List<OrderCreatedEvent.OrderItem> items) {
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, items);
        eventPublisher.publish(event);
        return orderId;
    }
}
