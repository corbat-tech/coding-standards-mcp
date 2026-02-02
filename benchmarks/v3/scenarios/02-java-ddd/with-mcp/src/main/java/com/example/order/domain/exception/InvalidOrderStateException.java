package com.example.order.domain.exception;

import com.example.order.domain.valueobject.OrderStatus;

public class InvalidOrderStateException extends DomainException {

    public InvalidOrderStateException(String operation, OrderStatus currentStatus) {
        super("Cannot " + operation + " when order is in " + currentStatus + " state");
    }
}
