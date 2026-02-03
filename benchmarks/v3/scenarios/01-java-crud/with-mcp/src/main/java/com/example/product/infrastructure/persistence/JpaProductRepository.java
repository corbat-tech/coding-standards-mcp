package com.example.product.infrastructure.persistence;

import com.example.product.domain.Product;
import com.example.product.domain.ProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProductRepository extends JpaRepository<Product, Long>, ProductRepository {
}
