package com.example.order.domain;

import com.example.order.domain.aggregate.Order;
import com.example.order.domain.entity.OrderItem;
import com.example.order.domain.event.DomainEvent;
import com.example.order.domain.event.OrderConfirmed;
import com.example.order.domain.event.OrderCreated;
import com.example.order.domain.exception.InvalidOrderStateException;
import com.example.order.domain.exception.MinimumOrderValueException;
import com.example.order.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Order aggregate.
 * Tests all business rules and invariants.
 */
class OrderTest {

    private static final String CUSTOMER_ID = "customer-123";
    private ProductId productId1;
    private ProductId productId2;

    @BeforeEach
    void setUp() {
        productId1 = ProductId.generate();
        productId2 = ProductId.generate();
    }

    @Nested
    @DisplayName("Order Creation Tests")
    class OrderCreationTests {

        @Test
        @DisplayName("Should create order in DRAFT status")
        void shouldCreateOrderInDraftStatus() {
            Order order = Order.create(CUSTOMER_ID);

            assertNotNull(order.getId());
            assertEquals(CUSTOMER_ID, order.getCustomerId());
            assertEquals(OrderStatus.DRAFT, order.getStatus());
            assertNotNull(order.getCreatedAt());
            assertTrue(order.isEmpty());
        }

        @Test
        @DisplayName("Should raise OrderCreated event on creation")
        void shouldRaiseOrderCreatedEvent() {
            Order order = Order.create(CUSTOMER_ID);

            List<DomainEvent> events = order.getDomainEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof OrderCreated);

            OrderCreated event = (OrderCreated) events.get(0);
            assertEquals(order.getId(), event.getOrderId());
            assertEquals(CUSTOMER_ID, event.getCustomerId());
        }

        @Test
        @DisplayName("Should throw exception for null customer ID")
        void shouldThrowExceptionForNullCustomerId() {
            assertThrows(NullPointerException.class, () -> Order.create(null));
        }

        @Test
        @DisplayName("Should throw exception for blank customer ID")
        void shouldThrowExceptionForBlankCustomerId() {
            assertThrows(IllegalArgumentException.class, () -> Order.create("  "));
        }
    }

    @Nested
    @DisplayName("Add Item Tests")
    class AddItemTests {

        @Test
        @DisplayName("Should add item to draft order")
        void shouldAddItemToDraftOrder() {
            Order order = Order.create(CUSTOMER_ID);

            order.addItem(productId1, "Product 1", Money.of(15.00), Quantity.of(2));

            assertEquals(1, order.getItemCount());
            assertEquals(2, order.getTotalQuantity());
            assertEquals(Money.of(30.00), order.calculateTotalValue());
        }

        @Test
        @DisplayName("Should increase quantity when adding same product")
        void shouldIncreaseQuantityWhenAddingSameProduct() {
            Order order = Order.create(CUSTOMER_ID);

            order.addItem(productId1, "Product 1", Money.of(10.00), Quantity.of(2));
            order.addItem(productId1, "Product 1", Money.of(10.00), Quantity.of(3));

            assertEquals(1, order.getItemCount());
            assertEquals(5, order.getTotalQuantity());
            assertEquals(Money.of(50.00), order.calculateTotalValue());
        }

        @Test
        @DisplayName("Should add multiple different products")
        void shouldAddMultipleDifferentProducts() {
            Order order = Order.create(CUSTOMER_ID);

            order.addItem(productId1, "Product 1", Money.of(10.00), Quantity.of(1));
            order.addItem(productId2, "Product 2", Money.of(20.00), Quantity.of(2));

            assertEquals(2, order.getItemCount());
            assertEquals(3, order.getTotalQuantity());
            assertEquals(Money.of(50.00), order.calculateTotalValue());
        }

        @Test
        @DisplayName("Should NOT allow adding items to confirmed order")
        void shouldNotAllowAddingItemsToConfirmedOrder() {
            Order order = createConfirmedOrder();

            assertThrows(InvalidOrderStateException.class, () ->
                    order.addItem(productId2, "Product 2", Money.of(5.00), Quantity.of(1)));
        }

        @Test
        @DisplayName("Should NOT allow adding items to shipped order")
        void shouldNotAllowAddingItemsToShippedOrder() {
            Order order = createShippedOrder();

            assertThrows(InvalidOrderStateException.class, () ->
                    order.addItem(productId2, "Product 2", Money.of(5.00), Quantity.of(1)));
        }

        @Test
        @DisplayName("Should NOT allow adding items to cancelled order")
        void shouldNotAllowAddingItemsToCancelledOrder() {
            Order order = Order.create(CUSTOMER_ID);
            order.cancel();

            assertThrows(InvalidOrderStateException.class, () ->
                    order.addItem(productId1, "Product 1", Money.of(10.00), Quantity.of(1)));
        }
    }

    @Nested
    @DisplayName("Remove Item Tests")
    class RemoveItemTests {

        @Test
        @DisplayName("Should remove item from draft order")
        void shouldRemoveItemFromDraftOrder() {
            Order order = Order.create(CUSTOMER_ID);
            order.addItem(productId1, "Product 1", Money.of(15.00), Quantity.of(1));
            order.addItem(productId2, "Product 2", Money.of(10.00), Quantity.of(1));

            order.removeItem(productId1);

            assertEquals(1, order.getItemCount());
            assertEquals(Money.of(10.00), order.calculateTotalValue());
        }

        @Test
        @DisplayName("Should NOT allow removing items from confirmed order")
        void shouldNotAllowRemovingItemsFromConfirmedOrder() {
            Order order = createConfirmedOrder();

            assertThrows(InvalidOrderStateException.class, () ->
                    order.removeItem(productId1));
        }
    }

    @Nested
    @DisplayName("Update Item Quantity Tests")
    class UpdateItemQuantityTests {

        @Test
        @DisplayName("Should update item quantity in draft order")
        void shouldUpdateItemQuantityInDraftOrder() {
            Order order = Order.create(CUSTOMER_ID);
            order.addItem(productId1, "Product 1", Money.of(10.00), Quantity.of(1));

            order.updateItemQuantity(productId1, Quantity.of(5));

            assertEquals(5, order.getTotalQuantity());
            assertEquals(Money.of(50.00), order.calculateTotalValue());
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent product")
        void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
            Order order = Order.create(CUSTOMER_ID);

            assertThrows(IllegalArgumentException.class, () ->
                    order.updateItemQuantity(productId1, Quantity.of(5)));
        }

        @Test
        @DisplayName("Should NOT allow updating quantity in confirmed order")
        void shouldNotAllowUpdatingQuantityInConfirmedOrder() {
            Order order = createConfirmedOrder();

            assertThrows(InvalidOrderStateException.class, () ->
                    order.updateItemQuantity(productId1, Quantity.of(10)));
        }
    }

    @Nested
    @DisplayName("Order Confirmation Tests")
    class OrderConfirmationTests {

        @Test
        @DisplayName("Should confirm order when value meets minimum")
        void shouldConfirmOrderWhenValueMeetsMinimum() {
            Order order = Order.create(CUSTOMER_ID);
            order.addItem(productId1, "Product 1", Money.of(10.00), Quantity.of(1));

            order.confirm();

            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
            assertTrue(order.getConfirmedAt().isPresent());
        }

        @Test
        @DisplayName("Should confirm order when value exceeds minimum")
        void shouldConfirmOrderWhenValueExceedsMinimum() {
            Order order = Order.create(CUSTOMER_ID);
            order.addItem(productId1, "Product 1", Money.of(50.00), Quantity.of(2));

            order.confirm();

            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        }

        @Test
        @DisplayName("Should raise OrderConfirmed event on confirmation")
        void shouldRaiseOrderConfirmedEvent() {
            Order order = Order.create(CUSTOMER_ID);
            order.addItem(productId1, "Product 1", Money.of(15.00), Quantity.of(1));

            order.confirm();

            List<DomainEvent> events = order.getDomainEvents();
            assertEquals(2, events.size()); // OrderCreated + OrderConfirmed

            DomainEvent lastEvent = events.get(1);
            assertTrue(lastEvent instanceof OrderConfirmed);

            OrderConfirmed confirmedEvent = (OrderConfirmed) lastEvent;
            assertEquals(order.getId(), confirmedEvent.getOrderId());
            assertEquals(Money.of(15.00), confirmedEvent.getTotalAmount());
            assertEquals(1, confirmedEvent.getItemCount());
        }

        @Test
        @DisplayName("Should NOT confirm order below minimum value")
        void shouldNotConfirmOrderBelowMinimumValue() {
            Order order = Order.create(CUSTOMER_ID);
            order.addItem(productId1, "Product 1", Money.of(5.00), Quantity.of(1));

            MinimumOrderValueException exception = assertThrows(
                    MinimumOrderValueException.class,
                    order::confirm);

            assertEquals(Money.of(5.00), exception.getCurrentValue());
            assertEquals(Money.of(10.00), exception.getMinimumValue());
            assertEquals(OrderStatus.DRAFT, order.getStatus());
        }

        @Test
        @DisplayName("Should NOT confirm empty order")
        void shouldNotConfirmEmptyOrder() {
            Order order = Order.create(CUSTOMER_ID);

            assertThrows(InvalidOrderStateException.class, order::confirm);
        }

        @Test
        @DisplayName("Should NOT confirm already confirmed order")
        void shouldNotConfirmAlreadyConfirmedOrder() {
            Order order = createConfirmedOrder();

            assertThrows(InvalidOrderStateException.class, order::confirm);
        }
    }

    @Nested
    @DisplayName("Order Shipping Tests")
    class OrderShippingTests {

        @Test
        @DisplayName("Should ship confirmed order")
        void shouldShipConfirmedOrder() {
            Order order = createConfirmedOrder();

            order.ship();

            assertEquals(OrderStatus.SHIPPED, order.getStatus());
            assertTrue(order.getShippedAt().isPresent());
        }

        @Test
        @DisplayName("Should NOT ship draft order")
        void shouldNotShipDraftOrder() {
            Order order = Order.create(CUSTOMER_ID);
            order.addItem(productId1, "Product 1", Money.of(15.00), Quantity.of(1));

            assertThrows(InvalidOrderStateException.class, order::ship);
        }

        @Test
        @DisplayName("Should NOT ship already shipped order")
        void shouldNotShipAlreadyShippedOrder() {
            Order order = createShippedOrder();

            assertThrows(InvalidOrderStateException.class, order::ship);
        }
    }

    @Nested
    @DisplayName("Order Delivery Tests")
    class OrderDeliveryTests {

        @Test
        @DisplayName("Should deliver shipped order")
        void shouldDeliverShippedOrder() {
            Order order = createShippedOrder();

            order.deliver();

            assertEquals(OrderStatus.DELIVERED, order.getStatus());
            assertTrue(order.getDeliveredAt().isPresent());
        }

        @Test
        @DisplayName("Should NOT deliver confirmed order")
        void shouldNotDeliverConfirmedOrder() {
            Order order = createConfirmedOrder();

            assertThrows(InvalidOrderStateException.class, order::deliver);
        }

        @Test
        @DisplayName("Should NOT deliver already delivered order")
        void shouldNotDeliverAlreadyDeliveredOrder() {
            Order order = createDeliveredOrder();

            assertThrows(InvalidOrderStateException.class, order::deliver);
        }
    }

    @Nested
    @DisplayName("Order Cancellation Tests")
    class OrderCancellationTests {

        @Test
        @DisplayName("Should cancel draft order")
        void shouldCancelDraftOrder() {
            Order order = Order.create(CUSTOMER_ID);

            order.cancel();

            assertEquals(OrderStatus.CANCELLED, order.getStatus());
            assertTrue(order.getCancelledAt().isPresent());
        }

        @Test
        @DisplayName("Should cancel confirmed order")
        void shouldCancelConfirmedOrder() {
            Order order = createConfirmedOrder();

            order.cancel();

            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }

        @Test
        @DisplayName("Should NOT cancel shipped order")
        void shouldNotCancelShippedOrder() {
            Order order = createShippedOrder();

            assertThrows(InvalidOrderStateException.class, order::cancel);
        }

        @Test
        @DisplayName("Should NOT cancel delivered order")
        void shouldNotCancelDeliveredOrder() {
            Order order = createDeliveredOrder();

            assertThrows(InvalidOrderStateException.class, order::cancel);
        }

        @Test
        @DisplayName("Should NOT cancel already cancelled order")
        void shouldNotCancelAlreadyCancelledOrder() {
            Order order = Order.create(CUSTOMER_ID);
            order.cancel();

            assertThrows(InvalidOrderStateException.class, order::cancel);
        }
    }

    @Nested
    @DisplayName("Domain Events Tests")
    class DomainEventsTests {

        @Test
        @DisplayName("Should pull and clear domain events")
        void shouldPullAndClearDomainEvents() {
            Order order = Order.create(CUSTOMER_ID);
            order.addItem(productId1, "Product 1", Money.of(15.00), Quantity.of(1));
            order.confirm();

            List<DomainEvent> events = order.pullDomainEvents();

            assertEquals(2, events.size());
            assertTrue(order.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("Events should have correct timestamps")
        void eventsShouldHaveCorrectTimestamps() {
            Order order = Order.create(CUSTOMER_ID);

            List<DomainEvent> events = order.getDomainEvents();
            DomainEvent createdEvent = events.get(0);

            assertNotNull(createdEvent.getEventId());
            assertNotNull(createdEvent.getOccurredOn());
            assertEquals("OrderCreated", createdEvent.getEventType());
        }
    }

    @Nested
    @DisplayName("Order State Queries Tests")
    class OrderStateQueriesTests {

        @Test
        @DisplayName("Draft order should be modifiable")
        void draftOrderShouldBeModifiable() {
            Order order = Order.create(CUSTOMER_ID);

            assertTrue(order.isModifiable());
        }

        @Test
        @DisplayName("Confirmed order should not be modifiable")
        void confirmedOrderShouldNotBeModifiable() {
            Order order = createConfirmedOrder();

            assertFalse(order.isModifiable());
        }

        @Test
        @DisplayName("Should return unmodifiable items list")
        void shouldReturnUnmodifiableItemsList() {
            Order order = Order.create(CUSTOMER_ID);
            order.addItem(productId1, "Product 1", Money.of(10.00), Quantity.of(1));

            List<OrderItem> items = order.getItems();

            assertThrows(UnsupportedOperationException.class, () ->
                    items.add(new OrderItem(productId2, "Product 2", Money.of(5.00), Quantity.of(1))));
        }
    }

    // Helper methods to create orders in various states

    private Order createConfirmedOrder() {
        Order order = Order.create(CUSTOMER_ID);
        order.addItem(productId1, "Product 1", Money.of(15.00), Quantity.of(1));
        order.confirm();
        return order;
    }

    private Order createShippedOrder() {
        Order order = createConfirmedOrder();
        order.ship();
        return order;
    }

    private Order createDeliveredOrder() {
        Order order = createShippedOrder();
        order.deliver();
        return order;
    }
}
