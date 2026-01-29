package application.service;

import application.command.AddItemCommand;
import application.command.ConfirmOrderCommand;
import application.command.CreateOrderCommand;
import domain.entity.Order;
import domain.valueobject.OrderId;

/**
 * Application service interface for Order operations.
 * Defines the use cases available for the Order aggregate.
 */
public interface OrderService {

    /**
     * Creates a new order for a customer.
     * @param command the create order command
     * @return the created order
     */
    Order createOrder(CreateOrderCommand command);

    /**
     * Adds an item to an existing order.
     * @param command the add item command
     * @return the updated order
     */
    Order addItem(AddItemCommand command);

    /**
     * Confirms an order if it meets all requirements.
     * @param command the confirm order command
     * @return the confirmed order
     */
    Order confirmOrder(ConfirmOrderCommand command);

    /**
     * Retrieves an order by its ID.
     * @param orderId the order ID
     * @return the order
     */
    Order getOrder(OrderId orderId);
}
