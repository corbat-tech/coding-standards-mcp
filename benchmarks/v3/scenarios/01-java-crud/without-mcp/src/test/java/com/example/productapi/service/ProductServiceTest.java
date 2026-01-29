package com.example.productapi.service;

import com.example.productapi.dto.ProductRequest;
import com.example.productapi.dto.ProductResponse;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.model.Product;
import com.example.productapi.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .category("Electronics")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productRequest = ProductRequest.builder()
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .category("Electronics")
                .build();
    }

    @Test
    @DisplayName("Should create product successfully")
    void createProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(productRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get product by ID successfully")
    void getProductById() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("Should throw exception when product not found by ID")
    void getProductByIdNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should get all products")
    void getAllProducts() {
        Product product2 = Product.builder()
                .id(2L)
                .name("Another Product")
                .description("Another Description")
                .price(new BigDecimal("49.99"))
                .category("Books")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productRepository.findAll()).thenReturn(Arrays.asList(product, product2));

        List<ProductResponse> products = productService.getAllProducts();

        assertThat(products).hasSize(2);
        assertThat(products).extracting(ProductResponse::getName)
                .containsExactly("Test Product", "Another Product");
    }

    @Test
    @DisplayName("Should get products by category")
    void getProductsByCategory() {
        when(productRepository.findByCategory("Electronics")).thenReturn(List.of(product));

        List<ProductResponse> products = productService.getProductsByCategory("Electronics");

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getCategory()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Should update product successfully")
    void updateProduct() {
        ProductRequest updateRequest = ProductRequest.builder()
                .name("Updated Product")
                .description("Updated Description")
                .price(new BigDecimal("149.99"))
                .category("Electronics")
                .build();

        Product updatedProduct = Product.builder()
                .id(1L)
                .name("Updated Product")
                .description("Updated Description")
                .price(new BigDecimal("149.99"))
                .category("Electronics")
                .createdAt(product.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        ProductResponse response = productService.updateProduct(1L, updateRequest);

        assertThat(response.getName()).isEqualTo("Updated Product");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("149.99"));
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent product")
    void updateProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, productRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should delete product successfully")
    void deleteProduct() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        productService.deleteProduct(1L);

        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent product")
    void deleteProductNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("99");
    }
}
