package com.example.order.domain;

import com.example.order.domain.exception.InvalidQuantityException;
import com.example.order.domain.valueobject.Quantity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class QuantityTest {

    @Test
    void shouldCreateQuantityWithValidValue() {
        Quantity quantity = Quantity.of(5);

        assertThat(quantity.getValue()).isEqualTo(5);
    }

    @Test
    void shouldThrowForZeroQuantity() {
        assertThatThrownBy(() -> Quantity.of(0))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void shouldThrowForNegativeQuantity() {
        assertThatThrownBy(() -> Quantity.of(-1))
                .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void shouldBeEqualForSameValue() {
        Quantity a = Quantity.of(3);
        Quantity b = Quantity.of(3);

        assertThat(a).isEqualTo(b);
    }
}
