package application.command;

import java.util.Objects;

/**
 * Command to create a new order.
 */
public final class CreateOrderCommand {

    private final String customerId;

    public CreateOrderCommand(String customerId) {
        this.customerId = Objects.requireNonNull(customerId, "CustomerId cannot be null");
    }

    public String getCustomerId() {
        return customerId;
    }
}
