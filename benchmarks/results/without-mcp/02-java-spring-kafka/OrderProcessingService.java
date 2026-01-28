package com.orders.service;

import com.orders.entity.ProcessedOrder;
import com.orders.event.OrderCreatedEvent;
import com.orders.repository.ProcessedOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OrderProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessingService.class);

    private final ProcessedOrderRepository processedOrderRepository;
    private final OrderValidationService orderValidationService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    @Autowired
    public OrderProcessingService(ProcessedOrderRepository processedOrderRepository,
                                  OrderValidationService orderValidationService,
                                  InventoryService inventoryService,
                                  NotificationService notificationService) {
        this.processedOrderRepository = processedOrderRepository;
        this.orderValidationService = orderValidationService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void processOrder(OrderCreatedEvent event) {
        logger.info("Processing order: {}", event.getOrderId());

        // Check for idempotency - skip if already processed
        if (isAlreadyProcessed(event.getOrderId())) {
            logger.warn("Order {} has already been processed. Skipping.", event.getOrderId());
            return;
        }

        // Mark as processing
        saveOrderStatus(event.getOrderId(), ProcessedOrder.OrderStatus.PROCESSING, null);

        try {
            // Step 1: Validate order
            orderValidationService.validate(event);

            // Step 2: Update inventory
            inventoryService.updateInventory(event);

            // Step 3: Send notification
            notificationService.sendOrderConfirmation(event);

            // Step 4: Save completed status
            saveOrderStatus(event.getOrderId(), ProcessedOrder.OrderStatus.COMPLETED, null);

            logger.info("Order {} processed successfully", event.getOrderId());

        } catch (Exception e) {
            logger.error("Failed to process order: {}", event.getOrderId(), e);
            saveOrderStatus(event.getOrderId(), ProcessedOrder.OrderStatus.FAILED, e.getMessage());
            throw e;
        }
    }

    public boolean isAlreadyProcessed(String orderId) {
        return processedOrderRepository.existsByOrderId(orderId);
    }

    private void saveOrderStatus(String orderId, ProcessedOrder.OrderStatus status, String errorMessage) {
        ProcessedOrder order = processedOrderRepository.findById(orderId)
                .orElse(new ProcessedOrder(orderId, status, Instant.now()));

        order.setStatus(status);
        order.setProcessedAt(Instant.now());
        order.setErrorMessage(errorMessage);

        processedOrderRepository.save(order);
    }
}
