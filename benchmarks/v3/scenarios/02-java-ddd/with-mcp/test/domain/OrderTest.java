package test.domain;

import domain.entity.Order;
import domain.entity.OrderItem;
import domain.event.DomainEvent;
import domain.event.OrderConfirmedEvent;
import domain.event.OrderCreatedEvent;
import domain.exception.InvalidOrderStateException;
import domain.exception.MinimumOrderValueException;
import domain.valueobject.Money;
import domain.valueobject.OrderStatus;
import domain.valueobject.ProductId;
import domain.valueobject.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Aggregate")
class OrderTest {

    private static final String CUSTOMER_ID = "customer-123";

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.create(CUSTOMER_ID);
    }

    private OrderItem createItem(double price, int quantity) {
        return new OrderItem(
            ProductId.generate(),
            "Test Product",
            Quantity.of(quantity),
            Money.of(BigDecimal.valueOf(price))
        );
    }

    @Nested
    @DisplayName("Order Creation")
    class OrderCreation {

        @Test
        @DisplayName("should create order with DRAFT status")
        void shouldCreateOrderWithDraftStatus() {
            assertEquals(OrderStatus.DRAFT, order.getStatus());
        }

        @Test
        @DisplayName("should create order with empty items list")
        void shouldCreateOrderWithEmptyItemsList() {
            assertTrue(order.isEmpty());
            assertEquals(0, order.getItemCount());
        }

        @Test
        @DisplayName("should generate OrderCreatedEvent when created")
        void shouldGenerateOrderCreatedEventWhenCreated() {
            List<DomainEvent> events = order.pullDomainEvents();

            assertEquals(1, events.size());
            assertInstanceOf(OrderCreatedEvent.class, events.get(0));

            OrderCreatedEvent event = (OrderCreatedEvent) events.get(0);
            assertEquals(order.getId(), event.getOrderId());
            assertEquals(CUSTOMER_ID, event.getCustomerId());
        }

        @Test
        @DisplayName("should clear events after pulling")
        void shouldClearEventsAfterPulling() {
            order.pullDomainEvents();
            List<DomainEvent> events = order.pullDomainEvents();

            assertTrue(events.isEmpty());
        }

        @Test
        @DisplayName("should assign customer ID correctly")
        void shouldAssignCustomerIdCorrectly() {
            assertEquals(CUSTOMER_ID, order.getCustomerId());
        }
    }

    @Nested
    @DisplayName("Adding Items")
    class AddingItems {

        @Test
        @DisplayName("should add item to draft order")
        void shouldAddItemToDraftOrder() {
            OrderItem item = createItem(15.00, 2);

            order.addItem(item);

            assertEquals(1, order.getItemCount());
            assertTrue(order.getItems().contains(item));
        }

        @Test
        @DisplayName("should add multiple items to order")
        void shouldAddMultipleItemsToOrder() {
            OrderItem item1 = createItem(10.00, 1);
            OrderItem item2 = createItem(20.00, 2);

            order.addItem(item1);
            order.addItem(item2);

            assertEquals(2, order.getItemCount());
        }

        @Test
        @DisplayName("should fail when adding item to confirmed order")
        void shouldFailWhenAddingItemToConfirmedOrder() {
            order.addItem(createItem(15.00, 1));
            order.confirm();
            order.pullDomainEvents();

            assertThrows(InvalidOrderStateException.class, () ->
                order.addItem(createItem(10.00, 1))
            );
        }

        @Test
        @DisplayName("should fail when adding item to shipped order")
        void shouldFailWhenAddingItemToShippedOrder() {
            order.addItem(createItem(15.00, 1));
            order.confirm();
            order.ship();

            assertThrows(InvalidOrderStateException.class, () ->
                order.addItem(createItem(10.00, 1))
            );
        }

        @Test
        @DisplayName("should reject null item")
        void shouldRejectNullItem() {
            assertThrows(NullPointerException.class, () ->
                order.addItem(null)
            );
        }
    }

    @Nested
    @DisplayName("Total Calculation")
    class TotalCalculation {

        @Test
        @DisplayName("should calculate total for single item")
        void shouldCalculateTotalForSingleItem() {
            order.addItem(createItem(10.00, 3));

            Money total = order.calculateTotal();

            assertEquals(BigDecimal.valueOf(30.00).setScale(2), total.getAmount());
        }

        @Test
        @DisplayName("should calculate total for multiple items")
        void shouldCalculateTotalForMultipleItems() {
            order.addItem(createItem(10.00, 2)); // 20.00
            order.addItem(createItem(5.50, 4));  // 22.00

            Money total = order.calculateTotal();

            assertEquals(BigDecimal.valueOf(42.00).setScale(2), total.getAmount());
        }

        @Test
        @DisplayName("should return zero for empty order")
        void shouldReturnZeroForEmptyOrder() {
            Money total = order.calculateTotal();

            assertEquals(BigDecimal.ZERO.setScale(2), total.getAmount());
        }
    }

    @Nested
    @DisplayName("Order Confirmation")
    class OrderConfirmation {

        @Test
        @DisplayName("should confirm order when minimum value is met")
        void shouldConfirmOrderWhenMinimumValueIsMet() {
            order.addItem(createItem(10.00, 1));

            order.confirm();

            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        }

        @Test
        @DisplayName("should confirm order when value exceeds minimum")
        void shouldConfirmOrderWhenValueExceedsMinimum() {
            order.addItem(createItem(50.00, 1));

            order.confirm();

            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        }

        @Test
        @DisplayName("should fail to confirm order below minimum value")
        void shouldFailToConfirmOrderBelowMinimumValue() {
            order.addItem(createItem(5.00, 1)); // $5 < $10 minimum

            MinimumOrderValueException exception = assertThrows(
                MinimumOrderValueException.class,
                () -> order.confirm()
            );

            assertEquals(Money.of(BigDecimal.valueOf(5.00)), exception.getCurrentTotal());
        }

        @Test
        @DisplayName("should fail to confirm empty order")
        void shouldFailToConfirmEmptyOrder() {
            assertThrows(MinimumOrderValueException.class, () ->
                order.confirm()
            );
        }

        @Test
        @DisplayName("should generate OrderConfirmedEvent when confirmed")
        void shouldGenerateOrderConfirmedEventWhenConfirmed() {
            order.addItem(createItem(15.00, 1));
            order.pullDomainEvents(); // Clear creation event

            order.confirm();
            List<DomainEvent> events = order.pullDomainEvents();

            assertEquals(1, events.size());
            assertInstanceOf(OrderConfirmedEvent.class, events.get(0));

            OrderConfirmedEvent event = (OrderConfirmedEvent) events.get(0);
            assertEquals(order.getId(), event.getOrderId());
            assertEquals(Money.of(BigDecimal.valueOf(15.00)), event.getTotalAmount());
            assertEquals(1, event.getItemCount());
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @BeforeEach
        void setUpWithItem() {
            order.addItem(createItem(15.00, 1));
        }

        @Test
        @DisplayName("should transition through valid states: DRAFT -> CONFIRMED -> SHIPPED -> DELIVERED")
        void shouldTransitionThroughValidStates() {
            assertEquals(OrderStatus.DRAFT, order.getStatus());

            order.confirm();
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());

            order.ship();
            assertEquals(OrderStatus.SHIPPED, order.getStatus());

            order.deliver();
            assertEquals(OrderStatus.DELIVERED, order.getStatus());
        }

        @Test
        @DisplayName("should cancel from DRAFT status")
        void shouldCancelFromDraftStatus() {
            order.cancel();

            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }

        @Test
        @DisplayName("should cancel from CONFIRMED status")
        void shouldCancelFromConfirmedStatus() {
            order.confirm();

            order.cancel();

            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }

        @Test
        @DisplayName("should fail to cancel from SHIPPED status")
        void shouldFailToCancelFromShippedStatus() {
            order.confirm();
            order.ship();

            assertThrows(InvalidOrderStateException.class, () ->
                order.cancel()
            );
        }

        @Test
        @DisplayName("should fail to ship from DRAFT status")
        void shouldFailToShipFromDraftStatus() {
            assertThrows(InvalidOrderStateException.class, () ->
                order.ship()
            );
        }

        @Test
        @DisplayName("should fail to deliver from CONFIRMED status")
        void shouldFailToDeliverFromConfirmedStatus() {
            order.confirm();

            assertThrows(InvalidOrderStateException.class, () ->
                order.deliver()
            );
        }
    }

    @Nested
    @DisplayName("Equality and Identity")
    class EqualityAndIdentity {

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            assertEquals(order, order);
        }

        @Test
        @DisplayName("should not be equal to different order")
        void shouldNotBeEqualToDifferentOrder() {
            Order anotherOrder = Order.create(CUSTOMER_ID);

            assertNotEquals(order, anotherOrder);
        }

        @Test
        @DisplayName("should return immutable items list")
        void shouldReturnImmutableItemsList() {
            order.addItem(createItem(10.00, 1));

            assertThrows(UnsupportedOperationException.class, () ->
                order.getItems().add(createItem(5.00, 1))
            );
        }
    }
}
