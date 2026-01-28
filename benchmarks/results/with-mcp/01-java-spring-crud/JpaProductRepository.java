package com.ecommerce.infrastructure.persistence;

import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.model.ProductCategory;
import com.ecommerce.domain.port.out.ProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JpaProductRepository extends JpaRepository<Product, Long>, ProductRepository {

    @Override
    List<Product> findByCategory(ProductCategory category);
}
