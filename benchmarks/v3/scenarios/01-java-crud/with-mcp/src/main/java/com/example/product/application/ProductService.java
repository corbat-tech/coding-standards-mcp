package com.example.product.application;

import com.example.product.domain.Product;
import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    Product create(String name, String description, BigDecimal price, String category);
    Product getById(Long id);
    List<Product> getAll();
    Product update(Long id, String name, String description, BigDecimal price, String category);
    void delete(Long id);
}
