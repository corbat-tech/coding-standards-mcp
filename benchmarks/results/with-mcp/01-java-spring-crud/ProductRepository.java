package com.ecommerce.domain.port.out;

import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.model.ProductCategory;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    List<Product> findByCategory(ProductCategory category);

    void deleteById(Long id);

    boolean existsById(Long id);
}
