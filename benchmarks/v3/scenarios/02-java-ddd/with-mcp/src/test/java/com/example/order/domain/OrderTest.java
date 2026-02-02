package com.example.order.domain;

import com.example.order.domain.entity.Order;
import com.example.order.domain.event.OrderConfirmedEvent;
import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.exception.InvalidOrderStateException;
import com.example.order.domain.exception.MinimumOrderValueException;
import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.OrderStatus;
import com.example.order.domain.valueobject.Quantity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    @Test
    void shouldCreateOrderInDraftState() {
        Order order = Order.create();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getId()).isNotNull();
    }

    @Test
    void shouldEmitOrderCreatedEventOnCreation() {
        Order order = Order.create();

        assertThat(order.getDomainEvents()).hasSize(1);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(OrderCreatedEvent.class);
    }

    @Test
    void shouldAddItemsToDraftOrder() {
        Order order = Order.create();

        order.addItem("P1", "Laptop", Money.of(999.99), Quantity.of(1));

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getProductName()).isEqualTo("Laptop");
    }

    @Test
    void shouldCalculateOrderTotal() {
        Order order = Order.create();
        order.addItem("P1", "Laptop", Money.of(100.00), Quantity.of(2));
        order.addItem("P2", "Mouse", Money.of(25.00), Quantity.of(1));

        Money total = order.calculateTotal();

        assertThat(total).isEqualTo(Money.of(225.00));
    }

    @Test
    void shouldConfirmOrderWithSufficientTotal() {
        Order order = Order.create();
        order.addItem("P1", "Item", Money.of(15.00), Quantity.of(1));

        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldEmitOrderConfirmedEventOnConfirmation() {
        Order order = Order.create();
        order.addItem("P1", "Item", Money.of(15.00), Quantity.of(1));
        order.clearDomainEvents();

        order.confirm();

        assertThat(order.getDomainEvents()).hasSize(1);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(OrderConfirmedEvent.class);
    }

    @Test
    void shouldNotConfirmOrderBelowMinimumValue() {
        Order order = Order.create();
        order.addItem("P1", "Cheap Item", Money.of(5.00), Quantity.of(1));

        assertThatThrownBy(order::confirm)
                .isInstanceOf(MinimumOrderValueException.class)
                .hasMessageContaining("$5.00")
                .hasMessageContaining("below minimum");
    }

    @Test
    void shouldNotAddItemsToConfirmedOrder() {
        Order order = Order.create();
        order.addItem("P1", "Item", Money.of(15.00), Quantity.of(1));
        order.confirm();

        assertThatThrownBy(() -> order.addItem("P2", "New Item", Money.of(10.00), Quantity.of(1)))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("add items")
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void shouldShipConfirmedOrder() {
        Order order = Order.create();
        order.addItem("P1", "Item", Money.of(15.00), Quantity.of(1));
        order.confirm();

        order.ship();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shouldNotShipDraftOrder() {
        Order order = Order.create();

        assertThatThrownBy(order::ship)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("ship")
                .hasMessageContaining("DRAFT");
    }

    @Test
    void shouldDeliverShippedOrder() {
        Order order = Order.create();
        order.addItem("P1", "Item", Money.of(15.00), Quantity.of(1));
        order.confirm();
        order.ship();

        order.deliver();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void shouldCancelDraftOrder() {
        Order order = Order.create();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldCancelConfirmedOrder() {
        Order order = Order.create();
        order.addItem("P1", "Item", Money.of(15.00), Quantity.of(1));
        order.confirm();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldNotCancelShippedOrder() {
        Order order = Order.create();
        order.addItem("P1", "Item", Money.of(15.00), Quantity.of(1));
        order.confirm();
        order.ship();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("cancel")
                .hasMessageContaining("SHIPPED");
    }
}
