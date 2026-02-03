package com.example.kafka.application.inventory;

import com.example.kafka.domain.event.OrderCreatedEvent;
import com.example.kafka.infrastructure.persistence.ProcessedEventRepository;
import com.example.kafka.infrastructure.persistence.ProcessedEventEntity;
import com.example.kafka.infrastructure.persistence.InventoryRepository;
import com.example.kafka.infrastructure.persistence.InventoryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                 ProcessedEventRepository processedEventRepository) {
        this.inventoryRepository = inventoryRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Override
    public void reserveStock(OrderCreatedEvent event) {
        if (isEventAlreadyProcessed(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }

        for (OrderCreatedEvent.OrderItem item : event.items()) {
            InventoryEntity inventory = inventoryRepository.findByProductId(item.productId())
                    .orElseGet(() -> createInventory(item.productId()));

            if (inventory.getAvailableQuantity() < item.quantity()) {
                throw new InsufficientStockException(item.productId(), item.quantity());
            }

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.quantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.quantity());
            inventoryRepository.save(inventory);
        }

        markEventAsProcessed(event.eventId());
        log.info("Stock reserved for order {}", event.orderId());
    }

    private boolean isEventAlreadyProcessed(String eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    private void markEventAsProcessed(String eventId) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        processedEventRepository.save(entity);
    }

    private InventoryEntity createInventory(String productId) {
        InventoryEntity entity = new InventoryEntity();
        entity.setProductId(productId);
        entity.setAvailableQuantity(100);
        entity.setReservedQuantity(0);
        return inventoryRepository.save(entity);
    }
}
