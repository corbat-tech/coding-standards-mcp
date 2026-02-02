package com.example.order.domain.exception;

public class InvalidQuantityException extends DomainException {

    public InvalidQuantityException(String message) {
        super(message);
    }
}
