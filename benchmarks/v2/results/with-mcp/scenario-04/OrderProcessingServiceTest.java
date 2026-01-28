package com.example.order.application;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.exception.DuplicateOrderException;
import com.example.order.domain.exception.OrderProcessingException;
import com.example.order.domain.model.ProcessedOrder;
import com.example.order.domain.model.ProcessingStatus;
import com.example.order.domain.port.Clock;
import com.example.order.domain.port.IdGenerator;
import com.example.order.domain.port.ProcessedOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private ProcessedOrderRepository repository;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private Clock clock;

    private OrderProcessingService service;

    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");
    private static final String GENERATED_ID = "processed-123";

    @BeforeEach
    void setUp() {
        service = new OrderProcessingService(repository, idGenerator, clock);
        when(idGenerator.generate()).thenReturn(GENERATED_ID);
        when(clock.now()).thenReturn(NOW);
    }

    @Test
    void should_process_order_when_valid_event() {
        // Arrange
        var event = createValidEvent();
        when(repository.existsByOrderId(event.orderId())).thenReturn(false);

        // Act
        ProcessedOrder result = service.process(event);

        // Assert
        assertThat(result.orderId()).isEqualTo(event.orderId());
        assertThat(result.status()).isEqualTo(ProcessingStatus.SUCCESS);
        assertThat(result.processedAt()).isEqualTo(NOW);
    }

    @Test
    void should_save_order_when_processed() {
        // Arrange
        var event = createValidEvent();
        when(repository.existsByOrderId(event.orderId())).thenReturn(false);

        // Act
        service.process(event);

        // Assert
        verify(repository).save(any(ProcessedOrder.class));
    }

    @Test
    void should_throw_DuplicateOrderException_when_order_exists() {
        // Arrange
        var event = createValidEvent();
        when(repository.existsByOrderId(event.orderId())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.process(event))
            .isInstanceOf(DuplicateOrderException.class)
            .hasMessageContaining(event.orderId());
    }

    @Test
    void should_throw_OrderProcessingException_when_no_items() {
        // Arrange
        var event = new OrderCreatedEvent(
            "order-123",
            "customer-456",
            List.of(),
            BigDecimal.valueOf(100),
            Instant.now()
        );
        when(repository.existsByOrderId(event.orderId())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> service.process(event))
            .isInstanceOf(OrderProcessingException.class)
            .hasMessageContaining("at least one item");
    }

    @Test
    void should_record_failure_when_processWithFailure_called() {
        // Arrange
        var event = createValidEvent();
        String failureReason = "Inventory not available";

        // Act
        ProcessedOrder result = service.processWithFailure(event, failureReason);

        // Assert
        assertThat(result.status()).isEqualTo(ProcessingStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(failureReason);
        verify(repository).save(any(ProcessedOrder.class));
    }

    @Test
    void should_not_retry_when_exception_is_not_retryable() {
        // Arrange
        var event = createValidEvent();
        when(repository.existsByOrderId(event.orderId())).thenReturn(true);

        // Act & Assert
        try {
            service.process(event);
        } catch (DuplicateOrderException e) {
            assertThat(e.isRetryable()).isFalse();
        }
    }

    private OrderCreatedEvent createValidEvent() {
        return new OrderCreatedEvent(
            "order-123",
            "customer-456",
            List.of(new OrderCreatedEvent.OrderItem("product-1", 2, BigDecimal.TEN)),
            BigDecimal.valueOf(20),
            Instant.now()
        );
    }
}
