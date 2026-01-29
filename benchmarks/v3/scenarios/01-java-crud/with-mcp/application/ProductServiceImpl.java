package com.example.products.application;

import com.example.products.domain.Product;
import com.example.products.domain.ProductRepository;
import com.example.products.domain.exception.ProductNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of ProductService.
 * Contains business logic for product operations.
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product createProduct(CreateProductCommand command) {
        Product product = new Product(
            command.name(),
            command.description(),
            command.price(),
            command.category()
        );
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Override
    public Product updateProduct(Long id, UpdateProductCommand command) {
        Product product = getProduct(id);
        product.setName(command.name());
        product.setDescription(command.description());
        product.setPrice(command.price());
        product.setCategory(command.category());
        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}
