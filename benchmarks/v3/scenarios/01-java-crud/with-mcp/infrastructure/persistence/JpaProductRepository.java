package com.example.products.infrastructure.persistence;

import com.example.products.domain.Product;
import com.example.products.domain.ProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA adapter for ProductRepository.
 * Infrastructure layer implementation of the domain port.
 */
@Repository
public interface JpaProductRepository extends JpaRepository<Product, Long>, ProductRepository {

    @Override
    List<Product> findByCategory(String category);
}
