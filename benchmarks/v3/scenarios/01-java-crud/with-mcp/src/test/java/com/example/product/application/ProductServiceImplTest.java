package com.example.product.application;

import com.example.product.domain.Product;
import com.example.product.domain.ProductRepository;
import com.example.product.domain.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository);
    }

    @Test
    void shouldCreateProduct() {
        Product product = new Product("Laptop", "High-end laptop", new BigDecimal("999.99"), "Electronics");
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.create("Laptop", "High-end laptop", new BigDecimal("999.99"), "Electronics");

        assertThat(result.getName()).isEqualTo("Laptop");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldGetProductById() {
        Product product = new Product("Laptop", "High-end laptop", new BigDecimal("999.99"), "Electronics");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getById(1L);

        assertThat(result.getName()).isEqualTo("Laptop");
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void shouldGetAllProducts() {
        List<Product> products = List.of(
                new Product("Laptop", "Desc", new BigDecimal("999.99"), "Electronics"),
                new Product("Phone", "Desc", new BigDecimal("599.99"), "Electronics")
        );
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldUpdateProduct() {
        Product product = new Product("Laptop", "Old desc", new BigDecimal("999.99"), "Electronics");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.update(1L, "Updated Laptop", "New desc", new BigDecimal("1099.99"), "Electronics");

        assertThat(result.getName()).isEqualTo("Updated Laptop");
        verify(productRepository).save(product);
    }

    @Test
    void shouldDeleteProduct() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentProduct() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(999L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
