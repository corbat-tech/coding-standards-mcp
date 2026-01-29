package test.domain;

import domain.entity.OrderItem;
import domain.valueobject.Money;
import domain.valueobject.ProductId;
import domain.valueobject.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderItem Entity")
class OrderItemTest {

    private static final ProductId PRODUCT_ID = ProductId.generate();
    private static final String PRODUCT_NAME = "Test Product";

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create order item with valid values")
        void shouldCreateOrderItemWithValidValues() {
            Quantity quantity = Quantity.of(2);
            Money unitPrice = Money.of(BigDecimal.valueOf(15.00));

            OrderItem item = new OrderItem(PRODUCT_ID, PRODUCT_NAME, quantity, unitPrice);

            assertEquals(PRODUCT_ID, item.getProductId());
            assertEquals(PRODUCT_NAME, item.getProductName());
            assertEquals(quantity, item.getQuantity());
            assertEquals(unitPrice, item.getUnitPrice());
        }

        @Test
        @DisplayName("should reject null product ID")
        void shouldRejectNullProductId() {
            assertThrows(NullPointerException.class, () ->
                new OrderItem(null, PRODUCT_NAME, Quantity.of(1), Money.of(BigDecimal.TEN))
            );
        }

        @Test
        @DisplayName("should reject null product name")
        void shouldRejectNullProductName() {
            assertThrows(NullPointerException.class, () ->
                new OrderItem(PRODUCT_ID, null, Quantity.of(1), Money.of(BigDecimal.TEN))
            );
        }

        @Test
        @DisplayName("should reject null quantity")
        void shouldRejectNullQuantity() {
            assertThrows(NullPointerException.class, () ->
                new OrderItem(PRODUCT_ID, PRODUCT_NAME, null, Money.of(BigDecimal.TEN))
            );
        }

        @Test
        @DisplayName("should reject null unit price")
        void shouldRejectNullUnitPrice() {
            assertThrows(NullPointerException.class, () ->
                new OrderItem(PRODUCT_ID, PRODUCT_NAME, Quantity.of(1), null)
            );
        }
    }

    @Nested
    @DisplayName("Subtotal Calculation")
    class SubtotalCalculation {

        @Test
        @DisplayName("should calculate subtotal correctly")
        void shouldCalculateSubtotalCorrectly() {
            OrderItem item = new OrderItem(
                PRODUCT_ID,
                PRODUCT_NAME,
                Quantity.of(3),
                Money.of(BigDecimal.valueOf(10.00))
            );

            Money subtotal = item.calculateSubtotal();

            assertEquals(BigDecimal.valueOf(30.00).setScale(2), subtotal.getAmount());
        }

        @Test
        @DisplayName("should calculate subtotal with decimal prices")
        void shouldCalculateSubtotalWithDecimalPrices() {
            OrderItem item = new OrderItem(
                PRODUCT_ID,
                PRODUCT_NAME,
                Quantity.of(4),
                Money.of(BigDecimal.valueOf(12.75))
            );

            Money subtotal = item.calculateSubtotal();

            assertEquals(BigDecimal.valueOf(51.00).setScale(2), subtotal.getAmount());
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal based on product ID")
        void shouldBeEqualBasedOnProductId() {
            OrderItem item1 = new OrderItem(
                PRODUCT_ID,
                PRODUCT_NAME,
                Quantity.of(1),
                Money.of(BigDecimal.TEN)
            );
            OrderItem item2 = new OrderItem(
                PRODUCT_ID,
                "Different Name",
                Quantity.of(5),
                Money.of(BigDecimal.valueOf(20))
            );

            assertEquals(item1, item2);
            assertEquals(item1.hashCode(), item2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different product IDs")
        void shouldNotBeEqualForDifferentProductIds() {
            OrderItem item1 = new OrderItem(
                ProductId.generate(),
                PRODUCT_NAME,
                Quantity.of(1),
                Money.of(BigDecimal.TEN)
            );
            OrderItem item2 = new OrderItem(
                ProductId.generate(),
                PRODUCT_NAME,
                Quantity.of(1),
                Money.of(BigDecimal.TEN)
            );

            assertNotEquals(item1, item2);
        }
    }
}
