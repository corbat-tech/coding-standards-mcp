package com.orders.consumer;

import com.orders.event.OrderCreatedEvent;
import com.orders.service.OrderProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);

    private final OrderProcessingService orderProcessingService;

    @Autowired
    public OrderCreatedEventConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @KafkaListener(
            topics = "orders.created",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, OrderCreatedEvent> record, Acknowledgment acknowledgment) {
        OrderCreatedEvent event = record.value();
        logger.info("Received order event: {} from partition: {} offset: {}",
                    event.getOrderId(), record.partition(), record.offset());

        try {
            orderProcessingService.processOrder(event);
            acknowledgment.acknowledge();
            logger.info("Successfully processed and acknowledged order: {}", event.getOrderId());

        } catch (Exception e) {
            logger.error("Error processing order: {}. Message will be retried.", event.getOrderId(), e);
            // Don't acknowledge - message will be redelivered or sent to DLQ
            throw e;
        }
    }
}
