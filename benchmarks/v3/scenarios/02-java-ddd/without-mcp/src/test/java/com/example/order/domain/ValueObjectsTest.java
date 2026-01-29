package com.example.order.domain;

import com.example.order.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all Value Objects in the Order domain.
 */
class ValueObjectsTest {

    @Nested
    @DisplayName("OrderId Value Object Tests")
    class OrderIdTests {

        @Test
        @DisplayName("Should generate unique OrderId")
        void shouldGenerateUniqueOrderId() {
            OrderId id1 = OrderId.generate();
            OrderId id2 = OrderId.generate();

            assertNotEquals(id1, id2);
        }

        @Test
        @DisplayName("Should create OrderId from UUID")
        void shouldCreateOrderIdFromUuid() {
            UUID uuid = UUID.randomUUID();
            OrderId orderId = OrderId.of(uuid);

            assertEquals(uuid, orderId.getValue());
        }

        @Test
        @DisplayName("Should create OrderId from string")
        void shouldCreateOrderIdFromString() {
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            OrderId orderId = OrderId.of(uuidString);

            assertEquals(uuidString, orderId.toString());
        }

        @Test
        @DisplayName("Should throw exception for null UUID")
        void shouldThrowExceptionForNullUuid() {
            assertThrows(NullPointerException.class, () -> OrderId.of((UUID) null));
        }

        @Test
        @DisplayName("Should throw exception for invalid UUID string")
        void shouldThrowExceptionForInvalidUuidString() {
            assertThrows(IllegalArgumentException.class, () -> OrderId.of("invalid-uuid"));
        }

        @Test
        @DisplayName("Equal OrderIds should have same hashCode")
        void equalOrderIdsShouldHaveSameHashCode() {
            UUID uuid = UUID.randomUUID();
            OrderId id1 = OrderId.of(uuid);
            OrderId id2 = OrderId.of(uuid);

            assertEquals(id1, id2);
            assertEquals(id1.hashCode(), id2.hashCode());
        }
    }

    @Nested
    @DisplayName("Money Value Object Tests")
    class MoneyTests {

        @Test
        @DisplayName("Should create Money with default currency")
        void shouldCreateMoneyWithDefaultCurrency() {
            Money money = Money.of(BigDecimal.valueOf(100));

            assertEquals(BigDecimal.valueOf(100).setScale(2), money.getAmount());
            assertEquals(Currency.getInstance("USD"), money.getCurrency());
        }

        @Test
        @DisplayName("Should create Money from double")
        void shouldCreateMoneyFromDouble() {
            Money money = Money.of(99.99);

            assertEquals(new BigDecimal("99.99"), money.getAmount());
        }

        @Test
        @DisplayName("Should create Money from string")
        void shouldCreateMoneyFromString() {
            Money money = Money.of("123.45");

            assertEquals(new BigDecimal("123.45"), money.getAmount());
        }

        @Test
        @DisplayName("Should throw exception for negative amount")
        void shouldThrowExceptionForNegativeAmount() {
            assertThrows(IllegalArgumentException.class, () -> Money.of(-10.00));
        }

        @Test
        @DisplayName("Should add money correctly")
        void shouldAddMoneyCorrectly() {
            Money money1 = Money.of(10.00);
            Money money2 = Money.of(20.00);

            Money result = money1.add(money2);

            assertEquals(Money.of(30.00), result);
        }

        @Test
        @DisplayName("Should subtract money correctly")
        void shouldSubtractMoneyCorrectly() {
            Money money1 = Money.of(30.00);
            Money money2 = Money.of(10.00);

            Money result = money1.subtract(money2);

            assertEquals(Money.of(20.00), result);
        }

        @Test
        @DisplayName("Should throw exception when subtracting to negative")
        void shouldThrowExceptionWhenSubtractingToNegative() {
            Money money1 = Money.of(10.00);
            Money money2 = Money.of(20.00);

            assertThrows(IllegalArgumentException.class, () -> money1.subtract(money2));
        }

        @Test
        @DisplayName("Should multiply money correctly")
        void shouldMultiplyMoneyCorrectly() {
            Money money = Money.of(10.00);

            Money result = money.multiply(3);

            assertEquals(Money.of(30.00), result);
        }

        @Test
        @DisplayName("Should throw exception for negative multiplier")
        void shouldThrowExceptionForNegativeMultiplier() {
            Money money = Money.of(10.00);

            assertThrows(IllegalArgumentException.class, () -> money.multiply(-1));
        }

        @Test
        @DisplayName("Should compare money values correctly")
        void shouldCompareMoneyValuesCorrectly() {
            Money money1 = Money.of(10.00);
            Money money2 = Money.of(20.00);

            assertTrue(money2.isGreaterThan(money1));
            assertTrue(money1.isLessThan(money2));
            assertTrue(money1.isGreaterThanOrEqual(Money.of(10.00)));
        }

        @Test
        @DisplayName("Should identify zero money")
        void shouldIdentifyZeroMoney() {
            assertTrue(Money.ZERO.isZero());
            assertFalse(Money.of(1.00).isZero());
        }

        @Test
        @DisplayName("Should throw exception for currency mismatch in operations")
        void shouldThrowExceptionForCurrencyMismatch() {
            Money usd = Money.of(BigDecimal.valueOf(10), Currency.getInstance("USD"));
            Money eur = Money.of(BigDecimal.valueOf(10), Currency.getInstance("EUR"));

            assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
        }

        @Test
        @DisplayName("Money should format correctly in toString")
        void moneyShouldFormatCorrectlyInToString() {
            Money money = Money.of(99.99);

            assertEquals("$99.99", money.toString());
        }
    }

    @Nested
    @DisplayName("Quantity Value Object Tests")
    class QuantityTests {

        @Test
        @DisplayName("Should create valid quantity")
        void shouldCreateValidQuantity() {
            Quantity quantity = Quantity.of(5);

            assertEquals(5, quantity.getValue());
        }

        @Test
        @DisplayName("Should throw exception for zero quantity")
        void shouldThrowExceptionForZeroQuantity() {
            assertThrows(IllegalArgumentException.class, () -> Quantity.of(0));
        }

        @Test
        @DisplayName("Should throw exception for negative quantity")
        void shouldThrowExceptionForNegativeQuantity() {
            assertThrows(IllegalArgumentException.class, () -> Quantity.of(-1));
        }

        @Test
        @DisplayName("Should add quantities correctly")
        void shouldAddQuantitiesCorrectly() {
            Quantity q1 = Quantity.of(3);
            Quantity q2 = Quantity.of(2);

            Quantity result = q1.add(q2);

            assertEquals(Quantity.of(5), result);
        }

        @Test
        @DisplayName("Should subtract quantities correctly")
        void shouldSubtractQuantitiesCorrectly() {
            Quantity q1 = Quantity.of(5);
            Quantity q2 = Quantity.of(2);

            Quantity result = q1.subtract(q2);

            assertEquals(Quantity.of(3), result);
        }

        @Test
        @DisplayName("Should throw exception when subtracting to zero or negative")
        void shouldThrowExceptionWhenSubtractingToZeroOrNegative() {
            Quantity q1 = Quantity.of(2);
            Quantity q2 = Quantity.of(2);

            assertThrows(IllegalArgumentException.class, () -> q1.subtract(q2));
        }

        @Test
        @DisplayName("Should compare quantities correctly")
        void shouldCompareQuantitiesCorrectly() {
            Quantity q1 = Quantity.of(5);
            Quantity q2 = Quantity.of(3);

            assertTrue(q1.isGreaterThan(q2));
            assertFalse(q2.isGreaterThan(q1));
        }

        @Test
        @DisplayName("Should have ONE constant")
        void shouldHaveOneConstant() {
            assertEquals(1, Quantity.ONE.getValue());
        }
    }

    @Nested
    @DisplayName("ProductId Value Object Tests")
    class ProductIdTests {

        @Test
        @DisplayName("Should generate unique ProductId")
        void shouldGenerateUniqueProductId() {
            ProductId id1 = ProductId.generate();
            ProductId id2 = ProductId.generate();

            assertNotEquals(id1, id2);
        }

        @Test
        @DisplayName("Should create ProductId from UUID")
        void shouldCreateProductIdFromUuid() {
            UUID uuid = UUID.randomUUID();
            ProductId productId = ProductId.of(uuid);

            assertEquals(uuid, productId.getValue());
        }

        @Test
        @DisplayName("Should throw exception for null UUID")
        void shouldThrowExceptionForNullUuid() {
            assertThrows(NullPointerException.class, () -> ProductId.of((UUID) null));
        }
    }

    @Nested
    @DisplayName("OrderStatus Value Object Tests")
    class OrderStatusTests {

        @Test
        @DisplayName("DRAFT can transition to CONFIRMED or CANCELLED")
        void draftCanTransitionToConfirmedOrCancelled() {
            assertTrue(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CONFIRMED));
            assertTrue(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CANCELLED));
            assertFalse(OrderStatus.DRAFT.canTransitionTo(OrderStatus.SHIPPED));
            assertFalse(OrderStatus.DRAFT.canTransitionTo(OrderStatus.DELIVERED));
        }

        @Test
        @DisplayName("CONFIRMED can transition to SHIPPED or CANCELLED")
        void confirmedCanTransitionToShippedOrCancelled() {
            assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPED));
            assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED));
            assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DRAFT));
            assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DELIVERED));
        }

        @Test
        @DisplayName("SHIPPED can only transition to DELIVERED")
        void shippedCanOnlyTransitionToDelivered() {
            assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
            assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
            assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("Terminal states cannot transition")
        void terminalStatesCannotTransition() {
            assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.DRAFT));
            assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DRAFT));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("Only DRAFT is modifiable")
        void onlyDraftIsModifiable() {
            assertTrue(OrderStatus.DRAFT.isModifiable());
            assertFalse(OrderStatus.CONFIRMED.isModifiable());
            assertFalse(OrderStatus.SHIPPED.isModifiable());
            assertFalse(OrderStatus.DELIVERED.isModifiable());
            assertFalse(OrderStatus.CANCELLED.isModifiable());
        }

        @Test
        @DisplayName("DELIVERED and CANCELLED are terminal")
        void deliveredAndCancelledAreTerminal() {
            assertTrue(OrderStatus.DELIVERED.isTerminal());
            assertTrue(OrderStatus.CANCELLED.isTerminal());
            assertFalse(OrderStatus.DRAFT.isTerminal());
            assertFalse(OrderStatus.CONFIRMED.isTerminal());
            assertFalse(OrderStatus.SHIPPED.isTerminal());
        }
    }
}
