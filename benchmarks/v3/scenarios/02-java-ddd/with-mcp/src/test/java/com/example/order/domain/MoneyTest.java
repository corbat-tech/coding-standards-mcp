package com.example.order.domain;

import com.example.order.domain.exception.InvalidMoneyException;
import com.example.order.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldCreateMoneyWithValidAmount() {
        Money money = Money.of(10.50);

        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("10.50"));
    }

    @Test
    void shouldThrowForNegativeAmount() {
        assertThatThrownBy(() -> Money.of(-1.00))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    void shouldThrowForNullAmount() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    void shouldAddMoney() {
        Money a = Money.of(10.00);
        Money b = Money.of(5.50);

        Money result = a.add(b);

        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("15.50"));
    }

    @Test
    void shouldMultiplyMoney() {
        Money money = Money.of(10.00);

        Money result = money.multiply(3);

        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void shouldDetectBelowMinimumOrderValue() {
        Money money = Money.of(5.00);

        assertThat(money.isLessThanMinimumOrder()).isTrue();
    }

    @Test
    void shouldDetectAboveMinimumOrderValue() {
        Money money = Money.of(15.00);

        assertThat(money.isLessThanMinimumOrder()).isFalse();
    }

    @Test
    void shouldBeEqualForSameAmount() {
        Money a = Money.of(10.00);
        Money b = Money.of(new BigDecimal("10.00"));

        assertThat(a).isEqualTo(b);
    }
}
