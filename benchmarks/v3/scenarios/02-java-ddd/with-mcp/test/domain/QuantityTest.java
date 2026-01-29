package test.domain;

import domain.exception.InvalidQuantityException;
import domain.valueobject.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Quantity Value Object")
class QuantityTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create quantity with positive value")
        void shouldCreateQuantityWithPositiveValue() {
            Quantity quantity = Quantity.of(5);

            assertEquals(5, quantity.getValue());
        }

        @Test
        @DisplayName("should reject zero quantity")
        void shouldRejectZeroQuantity() {
            assertThrows(InvalidQuantityException.class, () ->
                Quantity.of(0)
            );
        }

        @Test
        @DisplayName("should reject negative quantity")
        void shouldRejectNegativeQuantity() {
            assertThrows(InvalidQuantityException.class, () ->
                Quantity.of(-1)
            );
        }
    }

    @Nested
    @DisplayName("Operations")
    class Operations {

        @Test
        @DisplayName("should add two quantities")
        void shouldAddTwoQuantities() {
            Quantity a = Quantity.of(3);
            Quantity b = Quantity.of(5);

            Quantity result = a.add(b);

            assertEquals(8, result.getValue());
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal for same value")
        void shouldBeEqualForSameValue() {
            Quantity a = Quantity.of(5);
            Quantity b = Quantity.of(5);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different values")
        void shouldNotBeEqualForDifferentValues() {
            Quantity a = Quantity.of(5);
            Quantity b = Quantity.of(10);

            assertNotEquals(a, b);
        }
    }
}
