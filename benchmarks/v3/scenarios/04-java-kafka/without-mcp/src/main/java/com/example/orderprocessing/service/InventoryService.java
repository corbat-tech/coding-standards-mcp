package com.example.orderprocessing.service;

import com.example.orderprocessing.domain.entity.InventoryItem;
import com.example.orderprocessing.domain.entity.ProcessedEvent;
import com.example.orderprocessing.domain.event.OrderCreatedEvent;
import com.example.orderprocessing.domain.repository.InventoryRepository;
import com.example.orderprocessing.domain.repository.ProcessedEventRepository;
import com.example.orderprocessing.exception.InsufficientStockException;
import com.example.orderprocessing.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for inventory management.
 * Handles stock updates based on order events with idempotency guarantees.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private static final String CONSUMER_GROUP = "inventory-service";
    private static final String ORDER_CREATED_EVENT_TYPE = "OrderCreatedEvent";

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Process an OrderCreatedEvent with idempotency check.
     * Reserves stock for all items in the order.
     *
     * @param event The order created event
     * @return true if event was processed, false if it was a duplicate
     */
    @Transactional
    public boolean processOrderCreatedEvent(OrderCreatedEvent event) {
        String eventId = event.getEventId();

        // Idempotency check
        if (processedEventRepository.existsByEventId(eventId)) {
            log.warn("Event {} has already been processed, skipping", eventId);
            return false;
        }

        log.info("Processing OrderCreatedEvent: eventId={}, orderId={}",
                eventId, event.getOrderId());

        // Reserve stock for each item
        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            reserveStockForItem(item);
        }

        // Mark event as processed
        ProcessedEvent processedEvent = ProcessedEvent.create(
                eventId, ORDER_CREATED_EVENT_TYPE, CONSUMER_GROUP);
        processedEventRepository.save(processedEvent);

        log.info("Successfully processed OrderCreatedEvent: eventId={}", eventId);
        return true;
    }

    /**
     * Reserve stock for a single order item
     */
    private void reserveStockForItem(OrderCreatedEvent.OrderItem item) {
        String productId = item.getProductId();
        int quantity = item.getQuantity();

        InventoryItem inventoryItem = inventoryRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (!inventoryItem.reserveStock(quantity)) {
            throw new InsufficientStockException(
                    productId, quantity, inventoryItem.getEffectiveAvailable());
        }

        inventoryRepository.save(inventoryItem);
        log.info("Reserved {} units of product {}", quantity, productId);
    }

    /**
     * Add or update inventory for a product
     */
    @Transactional
    public InventoryItem addOrUpdateInventory(String productId, String productName, int quantity) {
        Optional<InventoryItem> existing = inventoryRepository.findById(productId);

        if (existing.isPresent()) {
            InventoryItem item = existing.get();
            item.setProductName(productName);
            item.setQuantityAvailable(item.getQuantityAvailable() + quantity);
            return inventoryRepository.save(item);
        } else {
            InventoryItem newItem = InventoryItem.builder()
                    .productId(productId)
                    .productName(productName)
                    .quantityAvailable(quantity)
                    .quantityReserved(0)
                    .build();
            return inventoryRepository.save(newItem);
        }
    }

    /**
     * Get inventory item by product ID
     */
    public Optional<InventoryItem> getInventory(String productId) {
        return inventoryRepository.findById(productId);
    }

    /**
     * Get all inventory items
     */
    public List<InventoryItem> getAllInventory() {
        return inventoryRepository.findAll();
    }

    /**
     * Get products with low stock
     */
    public List<InventoryItem> getLowStockItems(int threshold) {
        return inventoryRepository.findLowStockItems(threshold);
    }

    /**
     * Check if an event has already been processed
     */
    public boolean isEventProcessed(String eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }
}
