package com.example.productapi.repository;

import com.example.productapi.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .category("Electronics")
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve product by ID")
    void saveAndFindById() {
        Product savedProduct = entityManager.persistAndFlush(testProduct);

        Optional<Product> found = productRepository.findById(savedProduct.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("Should find products by category")
    void findByCategory() {
        entityManager.persistAndFlush(testProduct);

        Product anotherProduct = Product.builder()
                .name("Another Product")
                .description("Another Description")
                .price(new BigDecimal("49.99"))
                .category("Electronics")
                .build();
        entityManager.persistAndFlush(anotherProduct);

        Product differentCategory = Product.builder()
                .name("Different Product")
                .description("Different Description")
                .price(new BigDecimal("29.99"))
                .category("Books")
                .build();
        entityManager.persistAndFlush(differentCategory);

        List<Product> electronics = productRepository.findByCategory("Electronics");

        assertThat(electronics).hasSize(2);
        assertThat(electronics).extracting(Product::getName)
                .containsExactlyInAnyOrder("Test Product", "Another Product");
    }

    @Test
    @DisplayName("Should find products by name containing (case insensitive)")
    void findByNameContainingIgnoreCase() {
        entityManager.persistAndFlush(testProduct);

        Product laptop = Product.builder()
                .name("Gaming Laptop")
                .description("High performance laptop")
                .price(new BigDecimal("1499.99"))
                .category("Electronics")
                .build();
        entityManager.persistAndFlush(laptop);

        List<Product> results = productRepository.findByNameContainingIgnoreCase("test");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("Should check if product exists by name (case insensitive)")
    void existsByNameIgnoreCase() {
        entityManager.persistAndFlush(testProduct);

        boolean exists = productRepository.existsByNameIgnoreCase("test product");
        boolean notExists = productRepository.existsByNameIgnoreCase("nonexistent");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should delete product")
    void deleteProduct() {
        Product savedProduct = entityManager.persistAndFlush(testProduct);
        Long id = savedProduct.getId();

        productRepository.deleteById(id);
        entityManager.flush();

        Optional<Product> deleted = productRepository.findById(id);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Should update product")
    void updateProduct() {
        Product savedProduct = entityManager.persistAndFlush(testProduct);

        savedProduct.setName("Updated Name");
        savedProduct.setPrice(new BigDecimal("149.99"));
        productRepository.save(savedProduct);
        entityManager.flush();
        entityManager.clear();

        Product updated = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("149.99"));
    }
}
