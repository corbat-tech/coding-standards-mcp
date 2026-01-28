package com.example.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderProcessingService orderProcessingService;

    public OrderEventConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @KafkaListener(
        topics = "${kafka.topic.orders:orders}",
        groupId = "${kafka.consumer.group-id:order-processor}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(OrderEvent event, Acknowledgment acknowledgment) {
        logger.info("Received order event: {}", event.getOrderId());

        try {
            orderProcessingService.processOrder(event);
            acknowledgment.acknowledge();
            logger.info("Successfully processed order: {}", event.getOrderId());

        } catch (Exception e) {
            logger.error("Error processing order: {}. Will retry.", event.getOrderId(), e);
            throw e; // Let Kafka retry mechanism handle it
        }
    }
}
