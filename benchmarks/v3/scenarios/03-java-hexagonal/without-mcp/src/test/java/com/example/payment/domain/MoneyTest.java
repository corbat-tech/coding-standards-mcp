package com.example.payment.domain;

import com.example.payment.domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Money value object.
 */
@DisplayName("Money Value Object")
class MoneyTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create Money with valid amount and currency")
        void shouldCreateMoneyWithValidAmountAndCurrency() {
            Money money = Money.of(new BigDecimal("100.50"), "USD");

            assertEquals(new BigDecimal("100.50"), money.getAmount());
            assertEquals("USD", money.getCurrencyCode());
        }

        @Test
        @DisplayName("should scale amount to 2 decimal places")
        void shouldScaleAmountToTwoDecimalPlaces() {
            Money money = Money.of(new BigDecimal("100.555"), "USD");

            assertEquals(new BigDecimal("100.56"), money.getAmount());
        }

        @Test
        @DisplayName("should create zero Money")
        void shouldCreateZeroMoney() {
            Money money = Money.zero("EUR");

            assertTrue(money.isZero());
            assertEquals("EUR", money.getCurrencyCode());
        }

        @Test
        @DisplayName("should throw exception for null amount")
        void shouldThrowExceptionForNullAmount() {
            assertThrows(NullPointerException.class, () ->
                    Money.of(null, "USD"));
        }

        @Test
        @DisplayName("should throw exception for null currency")
        void shouldThrowExceptionForNullCurrency() {
            assertThrows(NullPointerException.class, () ->
                    Money.of(new BigDecimal("100"), (String) null));
        }

        @Test
        @DisplayName("should throw exception for negative amount")
        void shouldThrowExceptionForNegativeAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    Money.of(new BigDecimal("-10"), "USD"));
        }

        @Test
        @DisplayName("should throw exception for invalid currency code")
        void shouldThrowExceptionForInvalidCurrencyCode() {
            assertThrows(IllegalArgumentException.class, () ->
                    Money.of(new BigDecimal("100"), "INVALID"));
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperations {

        @Test
        @DisplayName("should add two Money objects with same currency")
        void shouldAddTwoMoneyObjectsWithSameCurrency() {
            Money a = Money.of(new BigDecimal("100.00"), "USD");
            Money b = Money.of(new BigDecimal("50.50"), "USD");

            Money result = a.add(b);

            assertEquals(new BigDecimal("150.50"), result.getAmount());
            assertEquals("USD", result.getCurrencyCode());
        }

        @Test
        @DisplayName("should throw exception when adding different currencies")
        void shouldThrowExceptionWhenAddingDifferentCurrencies() {
            Money usd = Money.of(new BigDecimal("100"), "USD");
            Money eur = Money.of(new BigDecimal("100"), "EUR");

            assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
        }

        @Test
        @DisplayName("should subtract two Money objects with same currency")
        void shouldSubtractTwoMoneyObjectsWithSameCurrency() {
            Money a = Money.of(new BigDecimal("100.00"), "USD");
            Money b = Money.of(new BigDecimal("30.50"), "USD");

            Money result = a.subtract(b);

            assertEquals(new BigDecimal("69.50"), result.getAmount());
        }

        @Test
        @DisplayName("should throw exception when subtraction results in negative")
        void shouldThrowExceptionWhenSubtractionResultsInNegative() {
            Money a = Money.of(new BigDecimal("50"), "USD");
            Money b = Money.of(new BigDecimal("100"), "USD");

            assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
        }

        @Test
        @DisplayName("should multiply Money by integer")
        void shouldMultiplyMoneyByInteger() {
            Money money = Money.of(new BigDecimal("25.50"), "USD");

            Money result = money.multiply(3);

            assertEquals(new BigDecimal("76.50"), result.getAmount());
        }

        @Test
        @DisplayName("should throw exception when multiplying by negative")
        void shouldThrowExceptionWhenMultiplyingByNegative() {
            Money money = Money.of(new BigDecimal("100"), "USD");

            assertThrows(IllegalArgumentException.class, () -> money.multiply(-1));
        }
    }

    @Nested
    @DisplayName("Comparisons")
    class Comparisons {

        @Test
        @DisplayName("should correctly compare greater than")
        void shouldCorrectlyCompareGreaterThan() {
            Money a = Money.of(new BigDecimal("100"), "USD");
            Money b = Money.of(new BigDecimal("50"), "USD");

            assertTrue(a.isGreaterThan(b));
            assertFalse(b.isGreaterThan(a));
        }

        @Test
        @DisplayName("should correctly compare greater than or equal")
        void shouldCorrectlyCompareGreaterThanOrEqual() {
            Money a = Money.of(new BigDecimal("100"), "USD");
            Money b = Money.of(new BigDecimal("100"), "USD");
            Money c = Money.of(new BigDecimal("50"), "USD");

            assertTrue(a.isGreaterThanOrEqual(b));
            assertTrue(a.isGreaterThanOrEqual(c));
            assertFalse(c.isGreaterThanOrEqual(a));
        }

        @Test
        @DisplayName("should throw exception when comparing different currencies")
        void shouldThrowExceptionWhenComparingDifferentCurrencies() {
            Money usd = Money.of(new BigDecimal("100"), "USD");
            Money eur = Money.of(new BigDecimal("100"), "EUR");

            assertThrows(IllegalArgumentException.class, () -> usd.isGreaterThan(eur));
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal for same amount and currency")
        void shouldBeEqualForSameAmountAndCurrency() {
            Money a = Money.of(new BigDecimal("100.00"), "USD");
            Money b = Money.of(new BigDecimal("100.00"), "USD");

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different amounts")
        void shouldNotBeEqualForDifferentAmounts() {
            Money a = Money.of(new BigDecimal("100.00"), "USD");
            Money b = Money.of(new BigDecimal("100.01"), "USD");

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("should not be equal for different currencies")
        void shouldNotBeEqualForDifferentCurrencies() {
            Money a = Money.of(new BigDecimal("100.00"), "USD");
            Money b = Money.of(new BigDecimal("100.00"), "EUR");

            assertNotEquals(a, b);
        }
    }
}
