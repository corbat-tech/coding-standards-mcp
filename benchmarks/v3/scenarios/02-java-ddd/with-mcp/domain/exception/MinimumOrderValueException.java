package domain.exception;

import domain.valueobject.Money;

/**
 * Exception thrown when an order does not meet the minimum value requirement.
 */
public class MinimumOrderValueException extends DomainException {

    private final Money currentTotal;
    private final Money minimumRequired;

    public MinimumOrderValueException(Money currentTotal, Money minimumRequired) {
        super("Order total " + currentTotal + " is below minimum required " + minimumRequired);
        this.currentTotal = currentTotal;
        this.minimumRequired = minimumRequired;
    }

    public Money getCurrentTotal() {
        return currentTotal;
    }

    public Money getMinimumRequired() {
        return minimumRequired;
    }
}
