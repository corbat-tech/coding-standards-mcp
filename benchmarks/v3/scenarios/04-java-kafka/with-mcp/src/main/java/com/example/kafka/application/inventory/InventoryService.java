package com.example.kafka.application.inventory;

import com.example.kafka.domain.event.OrderCreatedEvent;

public interface InventoryService {
    void reserveStock(OrderCreatedEvent event);
}
