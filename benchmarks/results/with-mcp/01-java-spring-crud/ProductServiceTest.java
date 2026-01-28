package com.ecommerce.application.service;

import com.ecommerce.application.dto.CreateProductRequest;
import com.ecommerce.application.dto.ProductResponse;
import com.ecommerce.application.dto.StockUpdateRequest;
import com.ecommerce.application.dto.StockUpdateRequest.StockOperation;
import com.ecommerce.application.dto.UpdateProductRequest;
import com.ecommerce.domain.exception.InsufficientStockException;
import com.ecommerce.domain.exception.ProductNotFoundException;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.model.ProductCategory;
import com.ecommerce.domain.port.out.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
            .name("Test Product")
            .description("Test Description")
            .price(new BigDecimal("99.99"))
            .stock(100)
            .category(ProductCategory.ELECTRONICS)
            .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should_create_product_when_valid_request")
        void should_create_product_when_valid_request() {
            // Arrange
            CreateProductRequest request = new CreateProductRequest(
                "New Product", "Description", new BigDecimal("49.99"), 50, ProductCategory.BOOKS);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            ProductResponse response = productService.create(request);

            // Assert
            assertThat(response).isNotNull();
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should_save_product_with_correct_values")
        void should_save_product_with_correct_values() {
            // Arrange
            CreateProductRequest request = new CreateProductRequest(
                "New Product", "Description", new BigDecimal("49.99"), 50, ProductCategory.BOOKS);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);

            // Act
            productService.create(request);

            // Assert
            verify(productRepository).save(captor.capture());
            Product saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("New Product");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should_return_product_when_exists")
        void should_return_product_when_exists() {
            // Arrange
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            // Act
            ProductResponse response = productService.findById(1L);

            // Assert
            assertThat(response.name()).isEqualTo(testProduct.getName());
        }

        @Test
        @DisplayName("should_throw_exception_when_product_not_found")
        void should_throw_exception_when_product_not_found() {
            // Arrange
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.findById(999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("findByCategory")
    class FindByCategory {

        @Test
        @DisplayName("should_return_products_when_category_matches")
        void should_return_products_when_category_matches() {
            // Arrange
            when(productRepository.findByCategory(ProductCategory.ELECTRONICS))
                .thenReturn(List.of(testProduct));

            // Act
            List<ProductResponse> response = productService.findByCategory(ProductCategory.ELECTRONICS);

            // Assert
            assertThat(response).hasSize(1);
        }

        @Test
        @DisplayName("should_return_empty_list_when_no_products_in_category")
        void should_return_empty_list_when_no_products_in_category() {
            // Arrange
            when(productRepository.findByCategory(ProductCategory.FOOD)).thenReturn(List.of());

            // Act
            List<ProductResponse> response = productService.findByCategory(ProductCategory.FOOD);

            // Assert
            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should_update_product_when_valid_request")
        void should_update_product_when_valid_request() {
            // Arrange
            UpdateProductRequest request = new UpdateProductRequest(
                "Updated Name", null, null, null, null);
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            ProductResponse response = productService.update(1L, request);

            // Assert
            assertThat(response).isNotNull();
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should_throw_exception_when_updating_nonexistent_product")
        void should_throw_exception_when_updating_nonexistent_product() {
            // Arrange
            UpdateProductRequest request = new UpdateProductRequest(
                "Updated Name", null, null, null, null);
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.update(999L, request))
                .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should_delete_product_when_exists")
        void should_delete_product_when_exists() {
            // Arrange
            when(productRepository.existsById(1L)).thenReturn(true);

            // Act
            productService.delete(1L);

            // Assert
            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should_throw_exception_when_deleting_nonexistent_product")
        void should_throw_exception_when_deleting_nonexistent_product() {
            // Arrange
            when(productRepository.existsById(999L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> productService.delete(999L))
                .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateStock")
    class UpdateStock {

        @Test
        @DisplayName("should_increment_stock_when_increment_operation")
        void should_increment_stock_when_increment_operation() {
            // Arrange
            StockUpdateRequest request = new StockUpdateRequest(10, StockOperation.INCREMENT);
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            productService.updateStock(1L, request);

            // Assert
            assertThat(testProduct.getStock()).isEqualTo(110);
        }

        @Test
        @DisplayName("should_decrement_stock_when_decrement_operation")
        void should_decrement_stock_when_decrement_operation() {
            // Arrange
            StockUpdateRequest request = new StockUpdateRequest(10, StockOperation.DECREMENT);
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            productService.updateStock(1L, request);

            // Assert
            assertThat(testProduct.getStock()).isEqualTo(90);
        }

        @Test
        @DisplayName("should_throw_exception_when_insufficient_stock")
        void should_throw_exception_when_insufficient_stock() {
            // Arrange
            StockUpdateRequest request = new StockUpdateRequest(150, StockOperation.DECREMENT);
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            // Act & Assert
            assertThatThrownBy(() -> productService.updateStock(1L, request))
                .isInstanceOf(InsufficientStockException.class);
        }
    }
}
