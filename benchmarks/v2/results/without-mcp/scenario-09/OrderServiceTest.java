package com.example.eventsourcing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private EventStore eventStore;
    private OrderProjection projection;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore();
        projection = new OrderProjection();
        orderService = new OrderService(eventStore, projection);
    }

    @Test
    void createOrder_CreatesOrderWithCorrectCustomer() {
        String orderId = orderService.createOrder("customer-1");

        Order order = orderService.getOrder(orderId);
        assertEquals("customer-1", order.getCustomerId());
        assertEquals(Order.OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void addItem_AddsItemToOrder() {
        String orderId = orderService.createOrder("customer-1");

        orderService.addItem(orderId, "product-1", "Widget", 2, new BigDecimal("10.00"));

        Order order = orderService.getOrder(orderId);
        assertEquals(1, order.getItems().size());
        assertEquals("Widget", order.getItems().get(0).getProductName());
        assertEquals(new BigDecimal("20.00"), order.getTotalAmount());
    }

    @Test
    void addItem_MultipleItems_CalculatesTotalCorrectly() {
        String orderId = orderService.createOrder("customer-1");

        orderService.addItem(orderId, "product-1", "Widget", 2, new BigDecimal("10.00"));
        orderService.addItem(orderId, "product-2", "Gadget", 1, new BigDecimal("25.00"));

        Order order = orderService.getOrder(orderId);
        assertEquals(2, order.getItems().size());
        assertEquals(new BigDecimal("45.00"), order.getTotalAmount());
    }

    @Test
    void shipOrder_ShipsOrder() {
        String orderId = orderService.createOrder("customer-1");
        orderService.addItem(orderId, "product-1", "Widget", 1, new BigDecimal("10.00"));

        orderService.shipOrder(orderId, "123 Main St", "TRACK-123");

        Order order = orderService.getOrder(orderId);
        assertEquals(Order.OrderStatus.SHIPPED, order.getStatus());
        assertEquals("123 Main St", order.getShippingAddress());
        assertEquals("TRACK-123", order.getTrackingNumber());
    }

    @Test
    void shipOrder_EmptyOrder_ThrowsException() {
        String orderId = orderService.createOrder("customer-1");

        assertThrows(IllegalStateException.class, () -> {
            orderService.shipOrder(orderId, "123 Main St", "TRACK-123");
        });
    }

    @Test
    void addItem_ToShippedOrder_ThrowsException() {
        String orderId = orderService.createOrder("customer-1");
        orderService.addItem(orderId, "product-1", "Widget", 1, new BigDecimal("10.00"));
        orderService.shipOrder(orderId, "123 Main St", "TRACK-123");

        assertThrows(IllegalStateException.class, () -> {
            orderService.addItem(orderId, "product-2", "Gadget", 1, new BigDecimal("25.00"));
        });
    }

    @Test
    void shipOrder_AlreadyShipped_ThrowsException() {
        String orderId = orderService.createOrder("customer-1");
        orderService.addItem(orderId, "product-1", "Widget", 1, new BigDecimal("10.00"));
        orderService.shipOrder(orderId, "123 Main St", "TRACK-123");

        assertThrows(IllegalStateException.class, () -> {
            orderService.shipOrder(orderId, "456 Other St", "TRACK-456");
        });
    }

    @Test
    void projection_TracksOrderCorrectly() {
        String orderId = orderService.createOrder("customer-1");
        orderService.addItem(orderId, "product-1", "Widget", 2, new BigDecimal("10.00"));
        orderService.shipOrder(orderId, "123 Main St", "TRACK-123");

        var view = projection.getOrder(orderId).orElseThrow();

        assertEquals("customer-1", view.getCustomerId());
        assertEquals(2, view.getItemCount());
        assertEquals(new BigDecimal("20.00"), view.getTotalAmount());
        assertEquals("SHIPPED", view.getStatus());
        assertEquals("TRACK-123", view.getTrackingNumber());
    }

    @Test
    void projection_FiltersByStatus() {
        String orderId1 = orderService.createOrder("customer-1");
        orderService.addItem(orderId1, "product-1", "Widget", 1, new BigDecimal("10.00"));
        orderService.shipOrder(orderId1, "123 Main St", "TRACK-123");

        String orderId2 = orderService.createOrder("customer-2");

        var shippedOrders = projection.getOrdersByStatus("SHIPPED");
        var createdOrders = projection.getOrdersByStatus("CREATED");

        assertEquals(1, shippedOrders.size());
        assertEquals(1, createdOrders.size());
    }

    @Test
    void reconstruct_RebuildsOrderFromEvents() {
        String orderId = orderService.createOrder("customer-1");
        orderService.addItem(orderId, "product-1", "Widget", 2, new BigDecimal("10.00"));
        orderService.addItem(orderId, "product-2", "Gadget", 1, new BigDecimal("25.00"));

        var events = eventStore.getEvents(orderId);
        Order reconstructed = Order.reconstruct(events);

        assertEquals(orderId, reconstructed.getOrderId());
        assertEquals("customer-1", reconstructed.getCustomerId());
        assertEquals(2, reconstructed.getItems().size());
        assertEquals(new BigDecimal("45.00"), reconstructed.getTotalAmount());
    }
}
