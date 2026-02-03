package com.example.product.infrastructure.web;

import com.example.product.application.ProductService;
import com.example.product.domain.Product;
import com.example.product.domain.exception.ProductNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void shouldCreateProduct() throws Exception {
        Product product = new Product("Laptop", "High-end laptop", new BigDecimal("999.99"), "Electronics");
        when(productService.create(any(), any(), any(), any())).thenReturn(product);

        CreateProductRequest request = new CreateProductRequest(
                "Laptop", "High-end laptop", new BigDecimal("999.99"), "Electronics");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void shouldReturnBadRequestForInvalidInput() throws Exception {
        CreateProductRequest request = new CreateProductRequest("", null, null, "");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void shouldGetProductById() throws Exception {
        Product product = new Product("Laptop", "High-end laptop", new BigDecimal("999.99"), "Electronics");
        when(productService.getById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentProduct() throws Exception {
        when(productService.getById(999L)).thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    void shouldGetAllProducts() throws Exception {
        List<Product> products = List.of(
                new Product("Laptop", "Desc", new BigDecimal("999.99"), "Electronics"),
                new Product("Phone", "Desc", new BigDecimal("599.99"), "Electronics")
        );
        when(productService.getAll()).thenReturn(products);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        Product product = new Product("Updated Laptop", "New desc", new BigDecimal("1099.99"), "Electronics");
        when(productService.update(eq(1L), any(), any(), any(), any())).thenReturn(product);

        UpdateProductRequest request = new UpdateProductRequest(
                "Updated Laptop", "New desc", new BigDecimal("1099.99"), "Electronics");

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Laptop"));
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
