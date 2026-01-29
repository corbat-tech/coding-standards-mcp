package domain.entity;

import domain.event.DomainEvent;
import domain.event.OrderConfirmedEvent;
import domain.event.OrderCreatedEvent;
import domain.exception.InvalidOrderStateException;
import domain.exception.MinimumOrderValueException;
import domain.valueobject.Money;
import domain.valueobject.OrderId;
import domain.valueobject.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Order Aggregate Root.
 * Enforces all business invariants for order management.
 */
public class Order {

    private static final Money MINIMUM_ORDER_VALUE = Money.of(BigDecimal.valueOf(10));

    private final OrderId id;
    private final String customerId;
    private final Instant createdAt;
    private final List<OrderItem> items;
    private final List<DomainEvent> domainEvents;
    private OrderStatus status;
    private Instant updatedAt;

    private Order(OrderId id, String customerId) {
        this.id = Objects.requireNonNull(id, "OrderId cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "CustomerId cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.items = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.status = OrderStatus.DRAFT;
    }

    /**
     * Factory method to create a new Order.
     * Raises OrderCreatedEvent.
     */
    public static Order create(String customerId) {
        OrderId orderId = OrderId.generate();
        Order order = new Order(orderId, customerId);
        order.registerEvent(new OrderCreatedEvent(orderId, customerId));
        return order;
    }

    /**
     * Factory method to reconstitute an Order from persistence.
     * Does not raise domain events.
     */
    public static Order reconstitute(
            OrderId id,
            String customerId,
            OrderStatus status,
            List<OrderItem> items,
            Instant createdAt,
            Instant updatedAt
    ) {
        Order order = new Order(id, customerId);
        order.status = status;
        order.items.addAll(items);
        return order;
    }

    /**
     * Adds an item to the order.
     * @throws InvalidOrderStateException if order is not in DRAFT status
     */
    public void addItem(OrderItem item) {
        validateModifiable();
        Objects.requireNonNull(item, "OrderItem cannot be null");
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    /**
     * Removes an item from the order by product ID.
     * @throws InvalidOrderStateException if order is not in DRAFT status
     */
    public void removeItem(OrderItem item) {
        validateModifiable();
        this.items.remove(item);
        this.updatedAt = Instant.now();
    }

    /**
     * Confirms the order.
     * @throws InvalidOrderStateException if order is not in DRAFT status
     * @throws MinimumOrderValueException if order total is below minimum
     */
    public void confirm() {
        validateModifiable();
        Money total = calculateTotal();

        if (total.isLessThan(MINIMUM_ORDER_VALUE)) {
            throw new MinimumOrderValueException(total, MINIMUM_ORDER_VALUE);
        }

        this.status = OrderStatus.DRAFT.transitionTo(OrderStatus.CONFIRMED);
        this.updatedAt = Instant.now();
        registerEvent(new OrderConfirmedEvent(id, total, items.size()));
    }

    /**
     * Ships the order.
     * @throws InvalidOrderStateException if order is not in CONFIRMED status
     */
    public void ship() {
        this.status = this.status.transitionTo(OrderStatus.SHIPPED);
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the order as delivered.
     * @throws InvalidOrderStateException if order is not in SHIPPED status
     */
    public void deliver() {
        this.status = this.status.transitionTo(OrderStatus.DELIVERED);
        this.updatedAt = Instant.now();
    }

    /**
     * Cancels the order.
     * @throws InvalidOrderStateException if order cannot be cancelled
     */
    public void cancel() {
        this.status = this.status.transitionTo(OrderStatus.CANCELLED);
        this.updatedAt = Instant.now();
    }

    /**
     * Calculates the total value of all items.
     */
    public Money calculateTotal() {
        return items.stream()
                .map(OrderItem::calculateSubtotal)
                .reduce(Money.zero(), Money::add);
    }

    private void validateModifiable() {
        if (!status.isModifiable()) {
            throw new InvalidOrderStateException(
                "Order cannot be modified in status: " + status
            );
        }
    }

    private void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * Returns and clears all pending domain events.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
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

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEmpty() {
        return items.isEmpty();
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
               ", total=" + calculateTotal() +
               '}';
    }
}
