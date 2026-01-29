package com.payment.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money Value Object")
class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create money with valid amount and currency")
        void shouldCreateMoneyWithValidAmountAndCurrency() {
            Money money = Money.of(new BigDecimal("100.00"), USD);

            assertThat(money.getAmount()).isEqualByComparingTo("100.00");
            assertThat(money.getCurrency()).isEqualTo(USD);
        }

        @Test
        @DisplayName("should create money using USD shorthand")
        void shouldCreateMoneyUsingUsdShorthand() {
            Money money = Money.usd(new BigDecimal("50.00"));

            assertThat(money.getCurrency()).isEqualTo(USD);
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("-10.00"), USD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount cannot be negative");
        }

        @Test
        @DisplayName("should reject null amount")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() -> Money.of(null, USD))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject null currency")
        void shouldRejectNullCurrency() {
            assertThatThrownBy(() -> Money.of(BigDecimal.TEN, (Currency) null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should scale amount to 2 decimal places")
        void shouldScaleAmountToTwoDecimalPlaces() {
            Money money = Money.of(new BigDecimal("100.999"), USD);

            assertThat(money.getAmount()).isEqualByComparingTo("101.00");
        }
    }

    @Nested
    @DisplayName("Operations")
    class Operations {

        @Test
        @DisplayName("should add two money values with same currency")
        void shouldAddTwoMoneyValuesWithSameCurrency() {
            Money a = Money.usd(new BigDecimal("100.00"));
            Money b = Money.usd(new BigDecimal("50.00"));

            Money result = a.add(b);

            assertThat(result.getAmount()).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("should subtract two money values with same currency")
        void shouldSubtractTwoMoneyValuesWithSameCurrency() {
            Money a = Money.usd(new BigDecimal("100.00"));
            Money b = Money.usd(new BigDecimal("30.00"));

            Money result = a.subtract(b);

            assertThat(result.getAmount()).isEqualByComparingTo("70.00");
        }

        @Test
        @DisplayName("should throw when subtracting results in negative")
        void shouldThrowWhenSubtractingResultsInNegative() {
            Money a = Money.usd(new BigDecimal("30.00"));
            Money b = Money.usd(new BigDecimal("50.00"));

            assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when operating on different currencies")
        void shouldThrowWhenOperatingOnDifferentCurrencies() {
            Money usd = Money.of(new BigDecimal("100.00"), USD);
            Money eur = Money.of(new BigDecimal("50.00"), EUR);

            assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
        }

        @Test
        @DisplayName("should compare money values correctly")
        void shouldCompareMoneyValuesCorrectly() {
            Money larger = Money.usd(new BigDecimal("100.00"));
            Money smaller = Money.usd(new BigDecimal("50.00"));

            assertThat(larger.isGreaterThan(smaller)).isTrue();
            assertThat(smaller.isGreaterThan(larger)).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal for same amount and currency")
        void shouldBeEqualForSameAmountAndCurrency() {
            Money a = Money.usd(new BigDecimal("100.00"));
            Money b = Money.usd(new BigDecimal("100.00"));

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different amounts")
        void shouldNotBeEqualForDifferentAmounts() {
            Money a = Money.usd(new BigDecimal("100.00"));
            Money b = Money.usd(new BigDecimal("50.00"));

            assertThat(a).isNotEqualTo(b);
        }
    }
}
