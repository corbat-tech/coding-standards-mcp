package com.example.order.domain.aggregate;

import com.example.order.domain.entity.OrderItem;
import com.example.order.domain.event.DomainEvent;
import com.example.order.domain.event.OrderConfirmed;
import com.example.order.domain.event.OrderCreated;
import com.example.order.domain.exception.InvalidOrderStateException;
import com.example.order.domain.exception.MinimumOrderValueException;
import com.example.order.domain.valueobject.*;

import java.time.Instant;
import java.util.*;

/**
 * Order Aggregate Root.
 *
 * This is the main aggregate that encapsulates all order-related business logic
 * and enforces invariants. All modifications to OrderItems must go through the Order.
 *
 * Invariants:
 * - Cannot add/remove items when order is not in DRAFT status
 * - Minimum order value of $10 must be met before confirmation
 * - Status transitions follow defined state machine rules
 */
public class Order {

    public static final Money MINIMUM_ORDER_VALUE = Money.of(10.00);

    private final OrderId id;
    private final String customerId;
    private final Instant createdAt;
    private final List<OrderItem> items;
    private final List<DomainEvent> domainEvents;

    private OrderStatus status;
    private Instant confirmedAt;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;

    /**
     * Creates a new Order in DRAFT status.
     *
     * @param customerId the customer placing the order
     */
    private Order(String customerId) {
        this.id = OrderId.generate();
        this.customerId = Objects.requireNonNull(customerId, "CustomerId cannot be null");
        this.createdAt = Instant.now();
        this.status = OrderStatus.DRAFT;
        this.items = new ArrayList<>();
        this.domainEvents = new ArrayList<>();

        if (customerId.isBlank()) {
            throw new IllegalArgumentException("CustomerId cannot be blank");
        }

        // Raise domain event
        this.domainEvents.add(new OrderCreated(this.id, this.customerId));
    }

    /**
     * Factory method to create a new Order.
     *
     * @param customerId the customer placing the order
     * @return a new Order in DRAFT status
     */
    public static Order create(String customerId) {
        return new Order(customerId);
    }

    /**
     * Adds an item to the order.
     *
     * @param productId the product identifier
     * @param productName the product name
     * @param unitPrice the unit price
     * @param quantity the quantity
     * @throws InvalidOrderStateException if order is not in DRAFT status
     */
    public void addItem(ProductId productId, String productName, Money unitPrice, Quantity quantity) {
        assertModifiable("add items");

        // Check if product already exists in order
        Optional<OrderItem> existingItem = findItemByProductId(productId);

        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity(quantity);
        } else {
            OrderItem newItem = new OrderItem(productId, productName, unitPrice, quantity);
            items.add(newItem);
        }
    }

    /**
     * Removes an item from the order.
     *
     * @param productId the product to remove
     * @throws InvalidOrderStateException if order is not in DRAFT status
     */
    public void removeItem(ProductId productId) {
        assertModifiable("remove items");
        items.removeIf(item -> item.getProductId().equals(productId));
    }

    /**
     * Updates the quantity of an existing item.
     *
     * @param productId the product to update
     * @param newQuantity the new quantity
     * @throws InvalidOrderStateException if order is not in DRAFT status
     * @throws IllegalArgumentException if product not found
     */
    public void updateItemQuantity(ProductId productId, Quantity newQuantity) {
        assertModifiable("update item quantity");

        OrderItem item = findItemByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found in order: " + productId));

        item.updateQuantity(newQuantity);
    }

    /**
     * Confirms the order, transitioning it from DRAFT to CONFIRMED status.
     *
     * @throws InvalidOrderStateException if order is not in DRAFT status
     * @throws MinimumOrderValueException if order value is below minimum
     */
    public void confirm() {
        assertCanTransitionTo(OrderStatus.CONFIRMED);

        Money totalValue = calculateTotalValue();

        if (totalValue.isLessThan(MINIMUM_ORDER_VALUE)) {
            throw new MinimumOrderValueException(totalValue, MINIMUM_ORDER_VALUE);
        }

        if (items.isEmpty()) {
            throw new InvalidOrderStateException(status, "confirm order with no items");
        }

        this.status = OrderStatus.CONFIRMED;
        this.confirmedAt = Instant.now();

        // Raise domain event
        this.domainEvents.add(new OrderConfirmed(this.id, totalValue, items.size()));
    }

    /**
     * Ships the order, transitioning it from CONFIRMED to SHIPPED status.
     *
     * @throws InvalidOrderStateException if order is not in CONFIRMED status
     */
    public void ship() {
        assertCanTransitionTo(OrderStatus.SHIPPED);
        this.status = OrderStatus.SHIPPED;
        this.shippedAt = Instant.now();
    }

    /**
     * Marks the order as delivered, transitioning it from SHIPPED to DELIVERED status.
     *
     * @throws InvalidOrderStateException if order is not in SHIPPED status
     */
    public void deliver() {
        assertCanTransitionTo(OrderStatus.DELIVERED);
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    /**
     * Cancels the order. Can only be cancelled from DRAFT or CONFIRMED status.
     *
     * @throws InvalidOrderStateException if order cannot be cancelled
     */
    public void cancel() {
        assertCanTransitionTo(OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    /**
     * Calculates the total value of all items in the order.
     *
     * @return the total order value
     */
    public Money calculateTotalValue() {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * Returns the number of items in the order.
     *
     * @return the item count
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Returns the total quantity of all items.
     *
     * @return the total quantity
     */
    public int getTotalQuantity() {
        return items.stream()
                .mapToInt(item -> item.getQuantity().getValue())
                .sum();
    }

    /**
     * Checks if the order is empty.
     *
     * @return true if the order has no items
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Checks if the order can be modified (items added/removed).
     *
     * @return true if the order is modifiable
     */
    public boolean isModifiable() {
        return status.isModifiable();
    }

    /**
     * Returns an unmodifiable view of the order items.
     *
     * @return the order items
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Returns and clears the domain events.
     *
     * @return the domain events
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    /**
     * Returns domain events without clearing them (for inspection).
     *
     * @return the domain events
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    // Getters

    public OrderId getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<Instant> getConfirmedAt() {
        return Optional.ofNullable(confirmedAt);
    }

    public Optional<Instant> getShippedAt() {
        return Optional.ofNullable(shippedAt);
    }

    public Optional<Instant> getDeliveredAt() {
        return Optional.ofNullable(deliveredAt);
    }

    public Optional<Instant> getCancelledAt() {
        return Optional.ofNullable(cancelledAt);
    }

    // Private helper methods

    private Optional<OrderItem> findItemByProductId(ProductId productId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    private void assertModifiable(String operation) {
        if (!status.isModifiable()) {
            throw new InvalidOrderStateException(status, operation);
        }
    }

    private void assertCanTransitionTo(OrderStatus targetStatus) {
        if (!status.canTransitionTo(targetStatus)) {
            throw new InvalidOrderStateException(status, targetStatus);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", customerId='" + customerId + '\'' +
                ", status=" + status +
                ", itemCount=" + items.size() +
                ", totalValue=" + calculateTotalValue() +
                '}';
    }
}
