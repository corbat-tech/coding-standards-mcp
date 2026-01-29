package com.example.products.application;

import com.example.products.domain.Product;
import com.example.products.domain.ProductRepository;
import com.example.products.domain.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository);
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should create product when valid data")
        void shouldCreateProductWhenValidData() {
            // Given
            CreateProductCommand command = new CreateProductCommand(
                "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"
            );
            Product savedProduct = new Product(
                "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"
            );
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // When
            Product result = productService.createProduct(command);

            // Then
            assertThat(result.getName()).isEqualTo("Laptop");
            assertThat(result.getPrice()).isEqualByComparingTo("999.99");
            verify(productRepository).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        @DisplayName("should return product when exists")
        void shouldReturnProductWhenExists() {
            // Given
            Long productId = 1L;
            Product product = new Product(
                "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"
            );
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // When
            Product result = productService.getProduct(productId);

            // Then
            assertThat(result.getName()).isEqualTo("Laptop");
        }

        @Test
        @DisplayName("should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            // Given
            Long productId = 999L;
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("getAllProducts")
    class GetAllProducts {

        @Test
        @DisplayName("should return all products")
        void shouldReturnAllProducts() {
            // Given
            List<Product> products = List.of(
                new Product("Laptop", "Desc", new BigDecimal("999.99"), "Electronics"),
                new Product("Phone", "Desc", new BigDecimal("599.99"), "Electronics")
            );
            when(productRepository.findAll()).thenReturn(products);

            // When
            List<Product> result = productService.getAllProducts();

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("should update product when exists")
        void shouldUpdateProductWhenExists() {
            // Given
            Long productId = 1L;
            Product existingProduct = new Product(
                "Laptop", "Old desc", new BigDecimal("999.99"), "Electronics"
            );
            UpdateProductCommand command = new UpdateProductCommand(
                "Laptop Pro", "New desc", new BigDecimal("1299.99"), "Electronics"
            );
            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Product result = productService.updateProduct(productId, command);

            // Then
            assertThat(result.getName()).isEqualTo("Laptop Pro");
            assertThat(result.getPrice()).isEqualByComparingTo("1299.99");
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("should delete product when exists")
        void shouldDeleteProductWhenExists() {
            // Given
            Long productId = 1L;
            when(productRepository.existsById(productId)).thenReturn(true);

            // When
            productService.deleteProduct(productId);

            // Then
            verify(productRepository).deleteById(productId);
        }

        @Test
        @DisplayName("should throw exception when deleting non-existent product")
        void shouldThrowExceptionWhenDeletingNonExistentProduct() {
            // Given
            Long productId = 999L;
            when(productRepository.existsById(productId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> productService.deleteProduct(productId))
                .isInstanceOf(ProductNotFoundException.class);
        }
    }
}
