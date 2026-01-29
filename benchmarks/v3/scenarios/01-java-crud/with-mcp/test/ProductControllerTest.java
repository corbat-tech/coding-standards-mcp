package com.example.products.infrastructure.web;

import com.example.products.application.CreateProductCommand;
import com.example.products.application.ProductService;
import com.example.products.domain.Product;
import com.example.products.domain.exception.ProductNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Nested
    @DisplayName("POST /api/products")
    class CreateProduct {

        @Test
        @DisplayName("should create product and return 201")
        void shouldCreateProductAndReturn201() throws Exception {
            // Given
            CreateProductRequest request = new CreateProductRequest(
                "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"
            );
            Product product = new Product(
                "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"
            );
            when(productService.createProduct(any(CreateProductCommand.class)))
                .thenReturn(product);

            // When & Then
            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99));
        }

        @Test
        @DisplayName("should return 400 when validation fails")
        void shouldReturn400WhenValidationFails() throws Exception {
            // Given - invalid request with blank name
            CreateProductRequest request = new CreateProductRequest(
                "", "Description", new BigDecimal("999.99"), "Electronics"
            );

            // When & Then
            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetProduct {

        @Test
        @DisplayName("should return product when exists")
        void shouldReturnProductWhenExists() throws Exception {
            // Given
            Product product = new Product(
                "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"
            );
            when(productService.getProduct(1L)).thenReturn(product);

            // When & Then
            mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenProductNotFound() throws Exception {
            // Given
            when(productService.getProduct(999L))
                .thenThrow(new ProductNotFoundException(999L));

            // When & Then
            mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product Not Found"));
        }
    }

    @Nested
    @DisplayName("GET /api/products")
    class GetAllProducts {

        @Test
        @DisplayName("should return all products")
        void shouldReturnAllProducts() throws Exception {
            // Given
            List<Product> products = List.of(
                new Product("Laptop", "Desc", new BigDecimal("999.99"), "Electronics"),
                new Product("Phone", "Desc", new BigDecimal("599.99"), "Electronics")
            );
            when(productService.getAllProducts()).thenReturn(products);

            // When & Then
            mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("should filter by category")
        void shouldFilterByCategory() throws Exception {
            // Given
            List<Product> products = List.of(
                new Product("Laptop", "Desc", new BigDecimal("999.99"), "Electronics")
            );
            when(productService.getProductsByCategory("Electronics")).thenReturn(products);

            // When & Then
            mockMvc.perform(get("/api/products").param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("should delete product and return 204")
        void shouldDeleteProductAndReturn204() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
        }
    }
}
