package com.example.saga.step;

import com.example.saga.application.port.InventoryService;
import com.example.saga.application.saga.CompensationResult;
import com.example.saga.application.saga.SagaContext;
import com.example.saga.application.saga.StepResult;
import com.example.saga.application.saga.step.ReserveInventoryStep;
import com.example.saga.domain.entity.Order;
import com.example.saga.domain.valueobject.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReserveInventoryStep")
class ReserveInventoryStepTest {

    @Mock
    private InventoryService inventoryService;

    private ReserveInventoryStep step;

    private List<OrderItem> testItems;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        step = new ReserveInventoryStep(inventoryService);

        testItems = Arrays.asList(
                new OrderItem("prod-1", 2, new BigDecimal("50.00"))
        );

        testOrder = Order.create(
                "order-123",
                "customer-456",
                testItems,
                new BigDecimal("100.00")
        );
    }

    @Test
    @DisplayName("should have correct step name")
    void shouldHaveCorrectStepName() {
        assertThat(step.getName()).isEqualTo("ReserveInventory");
    }

    @Nested
    @DisplayName("Execute")
    class Execute {

        @Test
        @DisplayName("should reserve inventory successfully")
        void shouldReserveInventorySuccessfully() {
            when(inventoryService.reserveInventory(anyString(), anyList()))
                    .thenReturn("reservation-789");

            SagaContext context = SagaContext.create("order-123")
                    .withOrder(testOrder);

            StepResult<String> result = step.execute(context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).contains("reservation-789");
            assertThat(result.getError()).isEmpty();

            verify(inventoryService).reserveInventory("order-123", testItems);
        }

        @Test
        @DisplayName("should fail when order is not in context")
        void shouldFailWhenOrderIsNotInContext() {
            SagaContext context = SagaContext.create("order-123");

            StepResult<String> result = step.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().getCode()).isEqualTo("ORDER_NOT_FOUND");
            assertThat(result.getError().get().isRetryable()).isFalse();

            verify(inventoryService, never()).reserveInventory(anyString(), anyList());
        }

        @Test
        @DisplayName("should fail when inventory service throws exception")
        void shouldFailWhenInventoryServiceThrowsException() {
            when(inventoryService.reserveInventory(anyString(), anyList()))
                    .thenThrow(new RuntimeException("Inventory unavailable"));

            SagaContext context = SagaContext.create("order-123")
                    .withOrder(testOrder);

            StepResult<String> result = step.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().getCode()).isEqualTo("INVENTORY_RESERVATION_FAILED");
            assertThat(result.getError().get().isRetryable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Compensate")
    class Compensate {

        @Test
        @DisplayName("should release inventory successfully")
        void shouldReleaseInventorySuccessfully() {
            SagaContext context = SagaContext.create("order-123")
                    .withInventoryReservation("reservation-789");

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getError()).isEmpty();

            verify(inventoryService).releaseInventory("reservation-789");
        }

        @Test
        @DisplayName("should succeed when no reservation in context")
        void shouldSucceedWhenNoReservationInContext() {
            SagaContext context = SagaContext.create("order-123");

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isTrue();

            verify(inventoryService, never()).releaseInventory(anyString());
        }

        @Test
        @DisplayName("should fail when inventory release throws exception")
        void shouldFailWhenInventoryReleaseThrowsException() {
            doThrow(new RuntimeException("Release failed"))
                    .when(inventoryService).releaseInventory(anyString());

            SagaContext context = SagaContext.create("order-123")
                    .withInventoryReservation("reservation-789");

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().getCode()).isEqualTo("INVENTORY_RELEASE_FAILED");
        }
    }
}
