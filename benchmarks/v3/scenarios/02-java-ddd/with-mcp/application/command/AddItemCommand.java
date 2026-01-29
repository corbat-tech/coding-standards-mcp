package application.command;

import domain.valueobject.OrderId;
import domain.valueobject.ProductId;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Command to add an item to an existing order.
 */
public final class AddItemCommand {

    private final OrderId orderId;
    private final ProductId productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;

    public AddItemCommand(
            OrderId orderId,
            ProductId productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {
        this.orderId = Objects.requireNonNull(orderId, "OrderId cannot be null");
        this.productId = Objects.requireNonNull(productId, "ProductId cannot be null");
        this.productName = Objects.requireNonNull(productName, "ProductName cannot be null");
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "UnitPrice cannot be null");
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
