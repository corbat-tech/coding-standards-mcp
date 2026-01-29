package test.domain;

import domain.exception.InvalidMoneyException;
import domain.valueobject.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Money Value Object")
class MoneyTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create money with valid positive amount")
        void shouldCreateMoneyWithValidPositiveAmount() {
            Money money = Money.of(BigDecimal.valueOf(100));

            assertEquals(BigDecimal.valueOf(100).setScale(2), money.getAmount());
        }

        @Test
        @DisplayName("should create money with zero amount")
        void shouldCreateMoneyWithZeroAmount() {
            Money money = Money.of(BigDecimal.ZERO);

            assertEquals(BigDecimal.ZERO.setScale(2), money.getAmount());
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThrows(InvalidMoneyException.class, () ->
                Money.of(BigDecimal.valueOf(-10))
            );
        }

        @Test
        @DisplayName("should reject null amount")
        void shouldRejectNullAmount() {
            assertThrows(NullPointerException.class, () ->
                Money.of(null)
            );
        }
    }

    @Nested
    @DisplayName("Operations")
    class Operations {

        @Test
        @DisplayName("should add two money values")
        void shouldAddTwoMoneyValues() {
            Money a = Money.of(BigDecimal.valueOf(10));
            Money b = Money.of(BigDecimal.valueOf(20));

            Money result = a.add(b);

            assertEquals(BigDecimal.valueOf(30).setScale(2), result.getAmount());
        }

        @Test
        @DisplayName("should multiply by quantity")
        void shouldMultiplyByQuantity() {
            Money unitPrice = Money.of(BigDecimal.valueOf(15.50));

            Money result = unitPrice.multiply(3);

            assertEquals(BigDecimal.valueOf(46.50).setScale(2), result.getAmount());
        }

        @Test
        @DisplayName("should reject negative multiplier")
        void shouldRejectNegativeMultiplier() {
            Money money = Money.of(BigDecimal.valueOf(10));

            assertThrows(InvalidMoneyException.class, () ->
                money.multiply(-1)
            );
        }
    }

    @Nested
    @DisplayName("Comparison")
    class Comparison {

        @Test
        @DisplayName("should compare greater than or equal correctly")
        void shouldCompareGreaterThanOrEqualCorrectly() {
            Money ten = Money.of(BigDecimal.valueOf(10));
            Money twenty = Money.of(BigDecimal.valueOf(20));

            assertTrue(twenty.isGreaterThanOrEqual(ten));
            assertTrue(ten.isGreaterThanOrEqual(ten));
            assertFalse(ten.isGreaterThanOrEqual(twenty));
        }

        @Test
        @DisplayName("should compare less than correctly")
        void shouldCompareLessThanCorrectly() {
            Money ten = Money.of(BigDecimal.valueOf(10));
            Money twenty = Money.of(BigDecimal.valueOf(20));

            assertTrue(ten.isLessThan(twenty));
            assertFalse(twenty.isLessThan(ten));
            assertFalse(ten.isLessThan(ten));
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal for same amount")
        void shouldBeEqualForSameAmount() {
            Money a = Money.of(BigDecimal.valueOf(10.00));
            Money b = Money.of(BigDecimal.valueOf(10.00));

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different amounts")
        void shouldNotBeEqualForDifferentAmounts() {
            Money a = Money.of(BigDecimal.valueOf(10.00));
            Money b = Money.of(BigDecimal.valueOf(20.00));

            assertNotEquals(a, b);
        }
    }
}
