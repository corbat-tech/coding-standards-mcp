package com.ecommerce.service;

import com.ecommerce.dto.ProductDTO;
import com.ecommerce.dto.StockUpdateRequest;
import com.ecommerce.exception.InvalidProductException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        product = new Product("Test Product", "Description", new BigDecimal("29.99"), 100, "Electronics");
        product.setId(1L);

        productDTO = new ProductDTO(null, "Test Product", "Description", new BigDecimal("29.99"), 100, "Electronics");
    }

    @Test
    void createProduct_ValidProduct_ReturnsCreatedProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDTO result = productService.createProduct(productDTO);

        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        assertEquals(new BigDecimal("29.99"), result.getPrice());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void createProduct_NullName_ThrowsException() {
        productDTO.setName(null);

        assertThrows(InvalidProductException.class, () -> productService.createProduct(productDTO));
    }

    @Test
    void createProduct_EmptyName_ThrowsException() {
        productDTO.setName("");

        assertThrows(InvalidProductException.class, () -> productService.createProduct(productDTO));
    }

    @Test
    void createProduct_NameTooLong_ThrowsException() {
        productDTO.setName("A".repeat(101));

        assertThrows(InvalidProductException.class, () -> productService.createProduct(productDTO));
    }

    @Test
    void createProduct_ZeroPrice_ThrowsException() {
        productDTO.setPrice(BigDecimal.ZERO);

        assertThrows(InvalidProductException.class, () -> productService.createProduct(productDTO));
    }

    @Test
    void createProduct_NegativePrice_ThrowsException() {
        productDTO.setPrice(new BigDecimal("-10.00"));

        assertThrows(InvalidProductException.class, () -> productService.createProduct(productDTO));
    }

    @Test
    void createProduct_NegativeStock_ThrowsException() {
        productDTO.setStock(-1);

        assertThrows(InvalidProductException.class, () -> productService.createProduct(productDTO));
    }

    @Test
    void getProductById_ExistingProduct_ReturnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDTO result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Product", result.getName());
    }

    @Test
    void getProductById_NonExistingProduct_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(999L));
    }

    @Test
    void getAllProducts_ReturnsAllProducts() {
        Product product2 = new Product("Product 2", "Desc 2", new BigDecimal("19.99"), 50, "Books");
        product2.setId(2L);
        when(productRepository.findAll()).thenReturn(Arrays.asList(product, product2));

        List<ProductDTO> results = productService.getAllProducts();

        assertEquals(2, results.size());
    }

    @Test
    void updateProduct_ValidUpdate_ReturnsUpdatedProduct() {
        ProductDTO updateDTO = new ProductDTO(null, "Updated Name", "Updated Desc", new BigDecimal("39.99"), 200, "Updated Category");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDTO result = productService.updateProduct(1L, updateDTO);

        assertNotNull(result);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProduct_NonExistingProduct_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(999L, productDTO));
    }

    @Test
    void deleteProduct_ExistingProduct_DeletesSuccessfully() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteProduct_NonExistingProduct_ThrowsException() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(999L));
    }

    @Test
    void getProductsByCategory_ReturnsMatchingProducts() {
        when(productRepository.findByCategoryIgnoreCase("Electronics")).thenReturn(Arrays.asList(product));

        List<ProductDTO> results = productService.getProductsByCategory("Electronics");

        assertEquals(1, results.size());
        assertEquals("Electronics", results.get(0).getCategory());
    }

    @Test
    void updateStock_IncrementStock_UpdatesCorrectly() {
        StockUpdateRequest request = new StockUpdateRequest(50, StockUpdateRequest.StockOperation.INCREMENT);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            return p;
        });

        ProductDTO result = productService.updateStock(1L, request);

        assertEquals(150, result.getStock());
    }

    @Test
    void updateStock_DecrementStock_UpdatesCorrectly() {
        StockUpdateRequest request = new StockUpdateRequest(30, StockUpdateRequest.StockOperation.DECREMENT);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            return p;
        });

        ProductDTO result = productService.updateStock(1L, request);

        assertEquals(70, result.getStock());
    }

    @Test
    void updateStock_InsufficientStock_ThrowsException() {
        StockUpdateRequest request = new StockUpdateRequest(150, StockUpdateRequest.StockOperation.DECREMENT);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductException.class, () -> productService.updateStock(1L, request));
    }

    @Test
    void updateStock_NonExistingProduct_ThrowsException() {
        StockUpdateRequest request = new StockUpdateRequest(10, StockUpdateRequest.StockOperation.INCREMENT);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateStock(999L, request));
    }
}
