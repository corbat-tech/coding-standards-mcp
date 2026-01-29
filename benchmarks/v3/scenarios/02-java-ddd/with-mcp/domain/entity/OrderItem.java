package domain.entity;

import domain.valueobject.Money;
import domain.valueobject.ProductId;
import domain.valueobject.Quantity;

import java.util.Objects;

/**
 * Entity representing a line item within an Order.
 * Contains product reference, quantity, and unit price.
 */
public final class OrderItem {

    private final ProductId productId;
    private final String productName;
    private final Quantity quantity;
    private final Money unitPrice;

    public OrderItem(ProductId productId, String productName, Quantity quantity, Money unitPrice) {
        this.productId = Objects.requireNonNull(productId, "ProductId cannot be null");
        this.productName = Objects.requireNonNull(productName, "ProductName cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "UnitPrice cannot be null");
    }

    public Money calculateSubtotal() {
        return unitPrice.multiply(quantity.getValue());
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(productId, orderItem.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
               "productId=" + productId +
               ", productName='" + productName + '\'' +
               ", quantity=" + quantity +
               ", unitPrice=" + unitPrice +
               '}';
    }
}
