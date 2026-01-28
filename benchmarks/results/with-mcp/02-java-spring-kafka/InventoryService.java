package com.orders.domain.service;

import com.orders.domain.event.OrderCreatedEvent;
import com.orders.domain.exception.InventoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    public void reserveInventory(OrderCreatedEvent event) {
        log.info("Reserving inventory for order: {}", event.orderId());

        for (OrderCreatedEvent.OrderItem item : event.items()) {
            reserveItem(event.orderId(), item);
        }

        log.info("Inventory reserved successfully for order: {}", event.orderId());
    }

    private void reserveItem(String orderId, OrderCreatedEvent.OrderItem item) {
        log.debug("Reserving {} units of product {} for order {}",
            item.quantity(), item.productId(), orderId);

        // In a real implementation, this would call the inventory system
        boolean reserved = checkAndReserve(item.productId(), item.quantity());

        if (!reserved) {
            throw new InventoryException(
                "Insufficient inventory for product: " + item.productId());
        }
    }

    private boolean checkAndReserve(String productId, Integer quantity) {
        // Simulated inventory check - in production, would call inventory service
        return true;
    }

    public void releaseInventory(OrderCreatedEvent event) {
        log.info("Releasing inventory for failed order: {}", event.orderId());
        // Release reserved inventory on failure
    }
}
