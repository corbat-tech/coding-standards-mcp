package domain.repository;

import domain.entity.Order;
import domain.valueobject.OrderId;

import java.util.Optional;

/**
 * Repository interface for Order aggregate persistence.
 * Part of the hexagonal architecture - this is a port.
 */
public interface OrderRepository {

    /**
     * Saves an order (create or update).
     * @param order the order to save
     * @return the saved order
     */
    Order save(Order order);

    /**
     * Finds an order by its ID.
     * @param orderId the order ID to search for
     * @return Optional containing the order if found
     */
    Optional<Order> findById(OrderId orderId);

    /**
     * Checks if an order exists.
     * @param orderId the order ID to check
     * @return true if the order exists
     */
    boolean existsById(OrderId orderId);

    /**
     * Deletes an order by ID.
     * @param orderId the order ID to delete
     */
    void deleteById(OrderId orderId);
}
