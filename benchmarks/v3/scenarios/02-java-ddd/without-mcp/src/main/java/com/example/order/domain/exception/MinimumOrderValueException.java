package com.example.order.domain.exception;

import com.example.order.domain.valueobject.Money;

/**
 * Exception thrown when an order does not meet the minimum value requirement.
 */
public class MinimumOrderValueException extends OrderDomainException {

    private final Money currentValue;
    private final Money minimumValue;

    public MinimumOrderValueException(Money currentValue, Money minimumValue) {
        super(String.format("Order value %s is below minimum required value of %s",
                currentValue, minimumValue));
        this.currentValue = currentValue;
        this.minimumValue = minimumValue;
    }

    public Money getCurrentValue() {
        return currentValue;
    }

    public Money getMinimumValue() {
        return minimumValue;
    }
}
