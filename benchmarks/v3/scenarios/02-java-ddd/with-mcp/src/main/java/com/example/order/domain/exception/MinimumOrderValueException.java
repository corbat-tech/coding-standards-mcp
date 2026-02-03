package com.example.order.domain.exception;

import com.example.order.domain.valueobject.Money;

public class MinimumOrderValueException extends DomainException {

    public MinimumOrderValueException(Money currentTotal) {
        super("Order total " + currentTotal + " is below minimum value of $10.00");
    }
}
