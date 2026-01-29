package test.application;

import application.command.AddItemCommand;
import application.command.ConfirmOrderCommand;
import application.command.CreateOrderCommand;
import application.port.DomainEventPublisher;
import application.service.OrderService;
import application.service.OrderServiceImpl;
import domain.entity.Order;
import domain.event.DomainEvent;
import domain.event.OrderConfirmedEvent;
import domain.event.OrderCreatedEvent;
import domain.exception.InvalidOrderStateException;
import domain.exception.MinimumOrderValueException;
import domain.exception.OrderNotFoundException;
import domain.repository.OrderRepository;
import domain.valueobject.Money;
import domain.valueobject.OrderId;
import domain.valueobject.OrderStatus;
import domain.valueobject.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, eventPublisher);
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("should create order and publish OrderCreatedEvent")
        void shouldCreateOrderAndPublishEvent() {
            when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            CreateOrderCommand command = new CreateOrderCommand("customer-123");

            Order result = orderService.createOrder(command);

            assertNotNull(result);
            assertEquals("customer-123", result.getCustomerId());
            assertEquals(OrderStatus.DRAFT, result.getStatus());

            verify(orderRepository).save(any(Order.class));

            ArgumentCaptor<List<DomainEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
            verify(eventPublisher).publishAll(eventsCaptor.capture());

            List<DomainEvent> events = eventsCaptor.getValue();
            assertEquals(1, events.size());
            assertInstanceOf(OrderCreatedEvent.class, events.get(0));
        }
    }

    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("should add item to existing order")
        void shouldAddItemToExistingOrder() {
            Order order = Order.create("customer-123");
            order.pullDomainEvents();

            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            AddItemCommand command = new AddItemCommand(
                order.getId(),
                ProductId.generate(),
                "Test Product",
                2,
                BigDecimal.valueOf(15.00)
            );

            Order result = orderService.addItem(command);

            assertEquals(1, result.getItemCount());
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("should throw OrderNotFoundException for non-existent order")
        void shouldThrowOrderNotFoundExceptionForNonExistentOrder() {
            OrderId orderId = OrderId.generate();
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            AddItemCommand command = new AddItemCommand(
                orderId,
                ProductId.generate(),
                "Test Product",
                1,
                BigDecimal.TEN
            );

            assertThrows(OrderNotFoundException.class, () ->
                orderService.addItem(command)
            );
        }

        @Test
        @DisplayName("should throw InvalidOrderStateException for confirmed order")
        void shouldThrowInvalidOrderStateExceptionForConfirmedOrder() {
            Order order = Order.create("customer-123");
            order.addItem(new domain.entity.OrderItem(
                ProductId.generate(),
                "Initial Product",
                domain.valueobject.Quantity.of(1),
                Money.of(BigDecimal.valueOf(15.00))
            ));
            order.confirm();
            order.pullDomainEvents();

            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            AddItemCommand command = new AddItemCommand(
                order.getId(),
                ProductId.generate(),
                "New Product",
                1,
                BigDecimal.TEN
            );

            assertThrows(InvalidOrderStateException.class, () ->
                orderService.addItem(command)
            );
        }
    }

    @Nested
    @DisplayName("confirmOrder")
    class ConfirmOrder {

        @Test
        @DisplayName("should confirm order and publish OrderConfirmedEvent")
        void shouldConfirmOrderAndPublishEvent() {
            Order order = Order.create("customer-123");
            order.addItem(new domain.entity.OrderItem(
                ProductId.generate(),
                "Test Product",
                domain.valueobject.Quantity.of(1),
                Money.of(BigDecimal.valueOf(15.00))
            ));
            order.pullDomainEvents();

            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            ConfirmOrderCommand command = new ConfirmOrderCommand(order.getId());

            Order result = orderService.confirmOrder(command);

            assertEquals(OrderStatus.CONFIRMED, result.getStatus());

            ArgumentCaptor<List<DomainEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
            verify(eventPublisher).publishAll(eventsCaptor.capture());

            List<DomainEvent> events = eventsCaptor.getValue();
            assertEquals(1, events.size());
            assertInstanceOf(OrderConfirmedEvent.class, events.get(0));
        }

        @Test
        @DisplayName("should throw MinimumOrderValueException for order below minimum")
        void shouldThrowMinimumOrderValueExceptionForOrderBelowMinimum() {
            Order order = Order.create("customer-123");
            order.addItem(new domain.entity.OrderItem(
                ProductId.generate(),
                "Cheap Product",
                domain.valueobject.Quantity.of(1),
                Money.of(BigDecimal.valueOf(5.00))
            ));
            order.pullDomainEvents();

            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            ConfirmOrderCommand command = new ConfirmOrderCommand(order.getId());

            assertThrows(MinimumOrderValueException.class, () ->
                orderService.confirmOrder(command)
            );

            verify(orderRepository, never()).save(any());
            verify(eventPublisher, never()).publishAll(any());
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("should return order when found")
        void shouldReturnOrderWhenFound() {
            Order order = Order.create("customer-123");
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            Order result = orderService.getOrder(order.getId());

            assertEquals(order, result);
        }

        @Test
        @DisplayName("should throw OrderNotFoundException when not found")
        void shouldThrowOrderNotFoundExceptionWhenNotFound() {
            OrderId orderId = OrderId.generate();
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class, () ->
                orderService.getOrder(orderId)
            );
        }
    }

    @Nested
    @DisplayName("Constructor Validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should reject null repository")
        void shouldRejectNullRepository() {
            assertThrows(NullPointerException.class, () ->
                new OrderServiceImpl(null, eventPublisher)
            );
        }

        @Test
        @DisplayName("should reject null event publisher")
        void shouldRejectNullEventPublisher() {
            assertThrows(NullPointerException.class, () ->
                new OrderServiceImpl(orderRepository, null)
            );
        }
    }
}
