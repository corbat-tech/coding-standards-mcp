package com.example.products.application;

import com.example.products.domain.Product;
import java.util.List;

/**
 * Application service interface for Product operations.
 * Defines the use cases for the product domain.
 */
public interface ProductService {

    Product createProduct(CreateProductCommand command);

    Product getProduct(Long id);

    List<Product> getAllProducts();

    List<Product> getProductsByCategory(String category);

    Product updateProduct(Long id, UpdateProductCommand command);

    void deleteProduct(Long id);
}
