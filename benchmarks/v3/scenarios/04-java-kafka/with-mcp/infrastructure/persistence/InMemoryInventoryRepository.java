package com.example.order.infrastructure.persistence;

import com.example.order.application.port.out.InventoryRepository;
import com.example.order.domain.model.InventoryItem;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of InventoryRepository for testing.
 * Thread-safe using ConcurrentHashMap.
 */
@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<String, InventoryItem> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<InventoryItem> findByProductId(String productId) {
        return Optional.ofNullable(storage.get(productId));
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        storage.put(item.getProductId(), item);
        return item;
    }

    @Override
    public boolean existsByProductId(String productId) {
        return storage.containsKey(productId);
    }

    /**
     * Seeds inventory data for testing.
     *
     * @param productId the product ID
     * @param productName the product name
     * @param quantity the initial available quantity
     */
    public void seedInventory(String productId, String productName, int quantity) {
        storage.put(productId, new InventoryItem(productId, productName, quantity));
    }

    /**
     * Clears all inventory data. Useful for test cleanup.
     */
    public void clear() {
        storage.clear();
    }
}
