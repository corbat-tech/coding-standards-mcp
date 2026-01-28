package com.orders.application.service;

import com.orders.domain.event.OrderCreatedEvent;
import com.orders.domain.model.ProcessedOrder;
import com.orders.domain.port.out.ProcessedOrderRepository;
import com.orders.domain.service.InventoryService;
import com.orders.domain.service.NotificationService;
import com.orders.domain.service.OrderValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    private final ProcessedOrderRepository orderRepository;
    private final OrderValidationService validationService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public OrderProcessingService(
            ProcessedOrderRepository orderRepository,
            OrderValidationService validationService,
            InventoryService inventoryService,
            NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.validationService = validationService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void processOrder(OrderCreatedEvent event) {
        log.info("Processing order: {}", event.orderId());

        if (isAlreadyProcessed(event.orderId())) {
            log.info("Order already processed, skipping: {}", event.orderId());
            return;
        }

        ProcessedOrder order = createOrGetOrder(event);
        order.markAsProcessing();
        orderRepository.save(order);

        try {
            executeProcessingSteps(event);
            completeOrder(order);
        } catch (Exception e) {
            handleProcessingFailure(order, event, e);
            throw e;
        }
    }

    private boolean isAlreadyProcessed(String orderId) {
        return orderRepository.findById(orderId)
            .map(ProcessedOrder::isAlreadyProcessed)
            .orElse(false);
    }

    private ProcessedOrder createOrGetOrder(OrderCreatedEvent event) {
        return orderRepository.findById(event.orderId())
            .orElseGet(() -> new ProcessedOrder(
                event.orderId(),
                event.customerId(),
                event.totalAmount(),
                event.timestamp()
            ));
    }

    private void executeProcessingSteps(OrderCreatedEvent event) {
        validationService.validate(event);
        inventoryService.reserveInventory(event);
        notificationService.sendOrderConfirmation(event);
    }

    private void completeOrder(ProcessedOrder order) {
        order.markAsCompleted();
        orderRepository.save(order);
        log.info("Order completed successfully: {}", order.getOrderId());
    }

    private void handleProcessingFailure(ProcessedOrder order, OrderCreatedEvent event, Exception e) {
        log.error("Order processing failed: {} - {}", order.getOrderId(), e.getMessage());
        order.markAsFailed(e.getMessage());
        orderRepository.save(order);
        inventoryService.releaseInventory(event);
        notificationService.sendOrderFailureNotification(
            order.getOrderId(), order.getCustomerId(), e.getMessage());
    }
}
