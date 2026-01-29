package domain.valueobject;

import domain.exception.InvalidOrderStateException;

import java.util.Set;
import java.util.Map;

/**
 * Enum representing the possible states of an Order.
 * Defines valid state transitions.
 */
public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
        DRAFT, Set.of(CONFIRMED, CANCELLED),
        CONFIRMED, Set.of(SHIPPED, CANCELLED),
        SHIPPED, Set.of(DELIVERED),
        DELIVERED, Set.of(),
        CANCELLED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public OrderStatus transitionTo(OrderStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidOrderStateException(
                "Cannot transition from " + this + " to " + target
            );
        }
        return target;
    }

    public boolean isModifiable() {
        return this == DRAFT;
    }

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
