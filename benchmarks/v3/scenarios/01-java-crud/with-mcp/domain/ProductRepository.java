package com.example.products.domain;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Product aggregate.
 * Follows hexagonal architecture - this is a port.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    List<Product> findByCategory(String category);

    void deleteById(Long id);

    boolean existsById(Long id);
}
