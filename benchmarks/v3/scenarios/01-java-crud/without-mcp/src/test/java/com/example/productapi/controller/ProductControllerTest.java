package com.example.productapi.controller;

import com.example.productapi.dto.ProductRequest;
import com.example.productapi.dto.ProductResponse;
import com.example.productapi.exception.GlobalExceptionHandler;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductRequest productRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productRequest = ProductRequest.builder()
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .category("Electronics")
                .build();

        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .category("Electronics")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/products - Should create product")
    void createProduct() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(productResponse);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.category").value("Electronics"));

        verify(productService, times(1)).createProduct(any(ProductRequest.class));
    }

    @Test
    @DisplayName("POST /api/products - Should return 400 for invalid request")
    void createProductInvalidRequest() throws Exception {
        ProductRequest invalidRequest = ProductRequest.builder()
                .name("") // Invalid: blank name
                .price(new BigDecimal("-10")) // Invalid: negative price
                .category("") // Invalid: blank category
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Should return product")
    void getProductById() throws Exception {
        when(productService.getProductById(1L)).thenReturn(productResponse);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Should return 404 for non-existent product")
    void getProductByIdNotFound() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Product")));
    }

    @Test
    @DisplayName("GET /api/products - Should return all products")
    void getAllProducts() throws Exception {
        ProductResponse product2 = ProductResponse.builder()
                .id(2L)
                .name("Another Product")
                .description("Another Description")
                .price(new BigDecimal("49.99"))
                .category("Books")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.getAllProducts()).thenReturn(Arrays.asList(productResponse, product2));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Test Product"))
                .andExpect(jsonPath("$[1].name").value("Another Product"));
    }

    @Test
    @DisplayName("GET /api/products?category= - Should filter by category")
    void getProductsByCategory() throws Exception {
        when(productService.getProductsByCategory("Electronics"))
                .thenReturn(Collections.singletonList(productResponse));

        mockMvc.perform(get("/api/products")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("Electronics"));

        verify(productService, times(1)).getProductsByCategory("Electronics");
    }

    @Test
    @DisplayName("PUT /api/products/{id} - Should update product")
    void updateProduct() throws Exception {
        ProductRequest updateRequest = ProductRequest.builder()
                .name("Updated Product")
                .description("Updated Description")
                .price(new BigDecimal("149.99"))
                .category("Electronics")
                .build();

        ProductResponse updatedResponse = ProductResponse.builder()
                .id(1L)
                .name("Updated Product")
                .description("Updated Description")
                .price(new BigDecimal("149.99"))
                .category("Electronics")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.updateProduct(eq(1L), any(ProductRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"))
                .andExpect(jsonPath("$.price").value(149.99));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - Should return 404 for non-existent product")
    void updateProductNotFound() throws Exception {
        when(productService.updateProduct(eq(99L), any(ProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(put("/api/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - Should delete product")
    void deleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService, times(1)).deleteProduct(1L);
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - Should return 404 for non-existent product")
    void deleteProductNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Product", "id", 99L))
                .when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Should return 400 for invalid ID format")
    void getProductByIdInvalidFormat() throws Exception {
        mockMvc.perform(get("/api/products/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
