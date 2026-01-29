package com.example.order.domain.valueobject;

/**
 * Enumeration representing the possible states of an Order.
 * Defines valid state transitions for the Order lifecycle.
 */
public enum OrderStatus {

    DRAFT("Draft - Order is being created"),
    CONFIRMED("Confirmed - Order has been confirmed and is ready for processing"),
    SHIPPED("Shipped - Order has been shipped to the customer"),
    DELIVERED("Delivered - Order has been delivered to the customer"),
    CANCELLED("Cancelled - Order has been cancelled");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if the order can transition to the given status.
     *
     * @param targetStatus the status to transition to
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(OrderStatus targetStatus) {
        return switch (this) {
            case DRAFT -> targetStatus == CONFIRMED || targetStatus == CANCELLED;
            case CONFIRMED -> targetStatus == SHIPPED || targetStatus == CANCELLED;
            case SHIPPED -> targetStatus == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    /**
     * Checks if the order is in a modifiable state (items can be added/removed).
     *
     * @return true if the order can be modified
     */
    public boolean isModifiable() {
        return this == DRAFT;
    }

    /**
     * Checks if the order is in a terminal state.
     *
     * @return true if the order is in a terminal state
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
