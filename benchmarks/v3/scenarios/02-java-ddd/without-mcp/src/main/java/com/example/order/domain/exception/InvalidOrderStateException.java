package com.example.order.domain.exception;

import com.example.order.domain.valueobject.OrderStatus;

/**
 * Exception thrown when an operation is attempted on an order in an invalid state.
 */
public class InvalidOrderStateException extends OrderDomainException {

    private final OrderStatus currentStatus;
    private final String attemptedOperation;

    public InvalidOrderStateException(OrderStatus currentStatus, String attemptedOperation) {
        super(String.format("Cannot %s: Order is in %s state", attemptedOperation, currentStatus));
        this.currentStatus = currentStatus;
        this.attemptedOperation = attemptedOperation;
    }

    public InvalidOrderStateException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super(String.format("Cannot transition from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.attemptedOperation = "transition to " + targetStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getAttemptedOperation() {
        return attemptedOperation;
    }
}
