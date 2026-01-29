package com.example.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InventoryItem")
class InventoryItemTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should create valid inventory item")
        void should_create_valid_item() {
            // When
            InventoryItem item = new InventoryItem("PROD-001", "Test Product", 100);

            // Then
            assertThat(item.getProductId()).isEqualTo("PROD-001");
            assertThat(item.getProductName()).isEqualTo("Test Product");
            assertThat(item.getAvailableQuantity()).isEqualTo(100);
            assertThat(item.getReservedQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("should reject negative available quantity")
        void should_reject_negative_quantity() {
            assertThatThrownBy(() -> new InventoryItem("PROD-001", "Test", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
        }
    }

    @Nested
    @DisplayName("reserveStock")
    class ReserveStock {

        @Test
        @DisplayName("should reserve stock successfully when available")
        void should_reserve_when_available() {
            // Given
            InventoryItem item = new InventoryItem("PROD-001", "Test", 100);

            // When
            boolean result = item.reserveStock(30);

            // Then
            assertThat(result).isTrue();
            assertThat(item.getAvailableQuantity()).isEqualTo(70);
            assertThat(item.getReservedQuantity()).isEqualTo(30);
        }

        @Test
        @DisplayName("should return false when insufficient stock")
        void should_return_false_when_insufficient() {
            // Given
            InventoryItem item = new InventoryItem("PROD-001", "Test", 10);

            // When
            boolean result = item.reserveStock(30);

            // Then
            assertThat(result).isFalse();
            assertThat(item.getAvailableQuantity()).isEqualTo(10);
            assertThat(item.getReservedQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("should reject non-positive quantity")
        void should_reject_non_positive() {
            InventoryItem item = new InventoryItem("PROD-001", "Test", 100);

            assertThatThrownBy(() -> item.reserveStock(0))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("releaseReservedStock")
    class ReleaseReservedStock {

        @Test
        @DisplayName("should release reserved stock back to available")
        void should_release_stock() {
            // Given
            InventoryItem item = new InventoryItem("PROD-001", "Test", 100);
            item.reserveStock(30);

            // When
            item.releaseReservedStock(20);

            // Then
            assertThat(item.getAvailableQuantity()).isEqualTo(90);
            assertThat(item.getReservedQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("should throw when releasing more than reserved")
        void should_throw_when_exceeding_reserved() {
            // Given
            InventoryItem item = new InventoryItem("PROD-001", "Test", 100);
            item.reserveStock(10);

            // When/Then
            assertThatThrownBy(() -> item.releaseReservedStock(20))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("confirmReservedStock")
    class ConfirmReservedStock {

        @Test
        @DisplayName("should confirm reserved stock as sold")
        void should_confirm_stock() {
            // Given
            InventoryItem item = new InventoryItem("PROD-001", "Test", 100);
            item.reserveStock(30);

            // When
            item.confirmReservedStock(20);

            // Then
            assertThat(item.getReservedQuantity()).isEqualTo(10);
            assertThat(item.getAvailableQuantity()).isEqualTo(70);
            assertThat(item.getTotalStock()).isEqualTo(80); // 70 + 10
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("should be equal based on productId")
        void should_equal_by_product_id() {
            InventoryItem item1 = new InventoryItem("PROD-001", "Test", 100);
            InventoryItem item2 = new InventoryItem("PROD-001", "Different", 50);

            assertThat(item1).isEqualTo(item2);
            assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        }

        @Test
        @DisplayName("should not be equal with different productId")
        void should_not_equal_different_id() {
            InventoryItem item1 = new InventoryItem("PROD-001", "Test", 100);
            InventoryItem item2 = new InventoryItem("PROD-002", "Test", 100);

            assertThat(item1).isNotEqualTo(item2);
        }
    }
}
