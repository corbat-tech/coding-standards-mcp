package com.orders.service;

import com.orders.event.OrderCreatedEvent;
import com.orders.exception.InventoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    public void updateInventory(OrderCreatedEvent event) {
        logger.info("Updating inventory for order: {}", event.getOrderId());

        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            try {
                decrementStock(item.getProductId(), item.getQuantity());
                logger.debug("Decremented stock for product {} by {}", item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                throw new InventoryException("Failed to update inventory for product: " + item.getProductId(), e);
            }
        }

        logger.info("Inventory updated successfully for order: {}", event.getOrderId());
    }

    private void decrementStock(String productId, int quantity) {
        // In a real implementation, this would interact with inventory database
        // For now, simulating inventory update
        if (productId == null) {
            throw new InventoryException("Product ID cannot be null");
        }
    }
}
