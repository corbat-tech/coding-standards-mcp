package com.example.order.application.service;

import com.example.order.application.port.in.ProcessOrderEventUseCase;
import com.example.order.application.port.out.InventoryRepository;
import com.example.order.application.port.out.ProcessedEventRepository;
import com.example.order.domain.events.OrderCreatedEvent;
import com.example.order.domain.exception.InsufficientStockException;
import com.example.order.domain.model.InventoryItem;
import com.example.order.domain.model.ProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for inventory operations.
 * Implements the ProcessOrderEventUseCase input port.
 * Handles idempotency through ProcessedEventRepository.
 */
@Service
public class InventoryService implements ProcessOrderEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String EVENT_TYPE = "OrderCreatedEvent";

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProcessedEventRepository processedEventRepository) {
        this.inventoryRepository = inventoryRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Override
    @Transactional
    public ProcessingResult processOrderCreated(OrderCreatedEvent event) {
        log.info("Processing OrderCreatedEvent: eventId={}, orderId={}",
            event.eventId(), event.orderId());

        if (isAlreadyProcessed(event.eventId())) {
            log.info("Event already processed, skipping: eventId={}", event.eventId());
            return ProcessingResult.skippedDuplicate(event.eventId());
        }

        try {
            reserveStockForOrder(event);
            recordSuccessfulProcessing(event.eventId());

            log.info("Successfully processed event: eventId={}", event.eventId());
            return ProcessingResult.success(event.eventId());

        } catch (InsufficientStockException e) {
            recordFailedProcessing(event.eventId(), e.getMessage());
            log.warn("Insufficient stock for event: eventId={}, message={}",
                event.eventId(), e.getMessage());
            return ProcessingResult.failure(event.eventId(), e.getMessage());

        } catch (Exception e) {
            recordFailedProcessing(event.eventId(), e.getMessage());
            log.error("Failed to process event: eventId={}", event.eventId(), e);
            throw e; // Re-throw for DLQ handling
        }
    }

    private boolean isAlreadyProcessed(String eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    private void reserveStockForOrder(OrderCreatedEvent event) {
        for (var item : event.items()) {
            reserveStockForItem(item);
        }
    }

    private void reserveStockForItem(OrderCreatedEvent.OrderItem item) {
        InventoryItem inventory = inventoryRepository.findByProductId(item.productId())
            .orElseThrow(() -> new InsufficientStockException(
                item.productId(), item.quantity(), 0));

        boolean reserved = inventory.reserveStock(item.quantity());
        if (!reserved) {
            throw new InsufficientStockException(
                item.productId(), item.quantity(), inventory.getAvailableQuantity());
        }

        inventoryRepository.save(inventory);
        log.debug("Reserved {} units of product {}", item.quantity(), item.productId());
    }

    private void recordSuccessfulProcessing(String eventId) {
        processedEventRepository.save(ProcessedEvent.success(eventId, EVENT_TYPE));
    }

    private void recordFailedProcessing(String eventId, String errorMessage) {
        processedEventRepository.save(ProcessedEvent.failure(eventId, EVENT_TYPE, errorMessage));
    }
}
