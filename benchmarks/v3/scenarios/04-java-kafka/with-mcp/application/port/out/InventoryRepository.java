package com.example.order.application.port.out;

import com.example.order.domain.model.InventoryItem;

import java.util.Optional;

/**
 * Output port for inventory persistence.
 * Implemented by infrastructure adapters.
 */
public interface InventoryRepository {

    /**
     * Finds an inventory item by product ID.
     *
     * @param productId the product ID to search for
     * @return an Optional containing the inventory item if found
     */
    Optional<InventoryItem> findByProductId(String productId);

    /**
     * Saves or updates an inventory item.
     *
     * @param item the inventory item to save
     * @return the saved inventory item
     */
    InventoryItem save(InventoryItem item);

    /**
     * Checks if an inventory item exists for the given product ID.
     *
     * @param productId the product ID to check
     * @return true if the item exists
     */
    boolean existsByProductId(String productId);
}
