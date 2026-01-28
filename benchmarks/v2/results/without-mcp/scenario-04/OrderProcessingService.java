package com.example.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessingService.class);

    private final OrderRepository orderRepository;

    public OrderProcessingService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public ProcessedOrder processOrder(OrderEvent event) {
        logger.info("Processing order: {}", event.getOrderId());

        // Check for duplicate
        if (orderRepository.existsByOrderId(event.getOrderId())) {
            logger.warn("Order already processed: {}", event.getOrderId());
            return orderRepository.findByOrderId(event.getOrderId()).orElseThrow();
        }

        try {
            validateOrder(event);

            ProcessedOrder processedOrder = new ProcessedOrder(
                event.getOrderId(),
                event.getCustomerId(),
                event.getTotalAmount(),
                ProcessedOrder.ProcessingStatus.PROCESSED
            );

            return orderRepository.save(processedOrder);

        } catch (Exception e) {
            logger.error("Failed to process order: {}", event.getOrderId(), e);

            ProcessedOrder failedOrder = new ProcessedOrder(
                event.getOrderId(),
                event.getCustomerId(),
                event.getTotalAmount(),
                ProcessedOrder.ProcessingStatus.FAILED
            );
            failedOrder.setErrorMessage(e.getMessage());

            return orderRepository.save(failedOrder);
        }
    }

    private void validateOrder(OrderEvent event) {
        if (event.getOrderId() == null || event.getOrderId().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (event.getCustomerId() == null || event.getCustomerId().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (event.getItems() == null || event.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        if (event.getTotalAmount() == null || event.getTotalAmount().signum() <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
    }
}
