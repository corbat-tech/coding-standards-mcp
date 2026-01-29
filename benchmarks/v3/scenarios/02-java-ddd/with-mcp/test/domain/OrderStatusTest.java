package test.domain;

import domain.exception.InvalidOrderStateException;
import domain.valueobject.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderStatus Enum")
class OrderStatusTest {

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("DRAFT can transition to CONFIRMED")
        void draftCanTransitionToConfirmed() {
            assertTrue(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CONFIRMED));

            OrderStatus result = OrderStatus.DRAFT.transitionTo(OrderStatus.CONFIRMED);

            assertEquals(OrderStatus.CONFIRMED, result);
        }

        @Test
        @DisplayName("DRAFT can transition to CANCELLED")
        void draftCanTransitionToCancelled() {
            assertTrue(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CANCELLED));
        }

        @Test
        @DisplayName("CONFIRMED can transition to SHIPPED")
        void confirmedCanTransitionToShipped() {
            assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPED));
        }

        @Test
        @DisplayName("CONFIRMED can transition to CANCELLED")
        void confirmedCanTransitionToCancelled() {
            assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED));
        }

        @Test
        @DisplayName("SHIPPED can transition to DELIVERED")
        void shippedCanTransitionToDelivered() {
            assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        }

        @Test
        @DisplayName("DRAFT cannot transition to SHIPPED")
        void draftCannotTransitionToShipped() {
            assertFalse(OrderStatus.DRAFT.canTransitionTo(OrderStatus.SHIPPED));

            assertThrows(InvalidOrderStateException.class, () ->
                OrderStatus.DRAFT.transitionTo(OrderStatus.SHIPPED)
            );
        }

        @Test
        @DisplayName("DELIVERED cannot transition to any state")
        void deliveredCannotTransitionToAnyState() {
            for (OrderStatus status : OrderStatus.values()) {
                assertFalse(OrderStatus.DELIVERED.canTransitionTo(status));
            }
        }

        @Test
        @DisplayName("CANCELLED cannot transition to any state")
        void cancelledCannotTransitionToAnyState() {
            for (OrderStatus status : OrderStatus.values()) {
                assertFalse(OrderStatus.CANCELLED.canTransitionTo(status));
            }
        }
    }

    @Nested
    @DisplayName("State Properties")
    class StateProperties {

        @Test
        @DisplayName("only DRAFT is modifiable")
        void onlyDraftIsModifiable() {
            assertTrue(OrderStatus.DRAFT.isModifiable());
            assertFalse(OrderStatus.CONFIRMED.isModifiable());
            assertFalse(OrderStatus.SHIPPED.isModifiable());
            assertFalse(OrderStatus.DELIVERED.isModifiable());
            assertFalse(OrderStatus.CANCELLED.isModifiable());
        }

        @Test
        @DisplayName("DELIVERED and CANCELLED are final states")
        void deliveredAndCancelledAreFinalStates() {
            assertFalse(OrderStatus.DRAFT.isFinal());
            assertFalse(OrderStatus.CONFIRMED.isFinal());
            assertFalse(OrderStatus.SHIPPED.isFinal());
            assertTrue(OrderStatus.DELIVERED.isFinal());
            assertTrue(OrderStatus.CANCELLED.isFinal());
        }
    }
}
