package com.example.kafka.infrastructure.kafka;

import com.example.kafka.application.inventory.InventoryService;
import com.example.kafka.domain.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderEventKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventKafkaConsumer.class);

    private final InventoryService inventoryService;

    public OrderEventKafkaConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(OrderCreatedEvent event, Acknowledgment ack) {
        log.info("Received OrderCreatedEvent for order {}", event.orderId());
        try {
            inventoryService.reserveStock(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process event for order {}", event.orderId(), e);
            throw e;
        }
    }
}
