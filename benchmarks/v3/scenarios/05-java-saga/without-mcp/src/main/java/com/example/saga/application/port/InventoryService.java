package com.example.saga.application.port;

import com.example.saga.domain.valueobject.OrderItem;

import java.util.List;

/**
 * Port interface for inventory management operations.
 */
public interface InventoryService {

    /**
     * Reserves inventory for the given order items.
     *
     * @param orderId the order ID
     * @param items   the items to reserve
     * @return the reservation ID
     */
    String reserveInventory(String orderId, List<OrderItem> items);

    /**
     * Releases a previously made inventory reservation.
     *
     * @param reservationId the reservation ID to release
     */
    void releaseInventory(String reservationId);

    /**
     * Checks if the given items are available in inventory.
     *
     * @param items the items to check
     * @return true if all items are available
     */
    boolean checkAvailability(List<OrderItem> items);
}
