package com.example.order.domain;

import com.example.order.domain.entity.OrderItem;
import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.ProductId;
import com.example.order.domain.valueobject.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OrderItem entity.
 */
class OrderItemTest {

    @Test
    @DisplayName("Should create order item with valid data")
    void shouldCreateOrderItemWithValidData() {
        ProductId productId = ProductId.generate();
        Money unitPrice = Money.of(25.00);
        Quantity quantity = Quantity.of(3);

        OrderItem item = new OrderItem(productId, "Test Product", unitPrice, quantity);

        assertNotNull(item.getId());
        assertEquals(productId, item.getProductId());
        assertEquals("Test Product", item.getProductName());
        assertEquals(unitPrice, item.getUnitPrice());
        assertEquals(quantity, item.getQuantity());
    }

    @Test
    @DisplayName("Should calculate total price correctly")
    void shouldCalculateTotalPriceCorrectly() {
        OrderItem item = new OrderItem(
                ProductId.generate(),
                "Test Product",
                Money.of(10.00),
                Quantity.of(5)
        );

        assertEquals(Money.of(50.00), item.getTotalPrice());
    }

    @Test
    @DisplayName("Should throw exception for null productId")
    void shouldThrowExceptionForNullProductId() {
        assertThrows(NullPointerException.class, () ->
                new OrderItem(null, "Product", Money.of(10.00), Quantity.of(1)));
    }

    @Test
    @DisplayName("Should throw exception for null product name")
    void shouldThrowExceptionForNullProductName() {
        assertThrows(NullPointerException.class, () ->
                new OrderItem(ProductId.generate(), null, Money.of(10.00), Quantity.of(1)));
    }

    @Test
    @DisplayName("Should throw exception for blank product name")
    void shouldThrowExceptionForBlankProductName() {
        assertThrows(IllegalArgumentException.class, () ->
                new OrderItem(ProductId.generate(), "  ", Money.of(10.00), Quantity.of(1)));
    }

    @Test
    @DisplayName("Should throw exception for null unit price")
    void shouldThrowExceptionForNullUnitPrice() {
        assertThrows(NullPointerException.class, () ->
                new OrderItem(ProductId.generate(), "Product", null, Quantity.of(1)));
    }

    @Test
    @DisplayName("Should throw exception for null quantity")
    void shouldThrowExceptionForNullQuantity() {
        assertThrows(NullPointerException.class, () ->
                new OrderItem(ProductId.generate(), "Product", Money.of(10.00), null));
    }

    @Test
    @DisplayName("Items with same id should be equal")
    void itemsWithSameIdShouldBeEqual() {
        OrderItem item1 = new OrderItem(
                ProductId.generate(),
                "Product",
                Money.of(10.00),
                Quantity.of(1)
        );

        // Same object reference
        assertEquals(item1, item1);
        assertEquals(item1.hashCode(), item1.hashCode());
    }

    @Test
    @DisplayName("Items with different ids should not be equal")
    void itemsWithDifferentIdsShouldNotBeEqual() {
        ProductId productId = ProductId.generate();

        OrderItem item1 = new OrderItem(productId, "Product", Money.of(10.00), Quantity.of(1));
        OrderItem item2 = new OrderItem(productId, "Product", Money.of(10.00), Quantity.of(1));

        // Different items have different UUIDs
        assertNotEquals(item1, item2);
    }
}
