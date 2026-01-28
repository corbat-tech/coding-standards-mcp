package com.ecommerce.application.service;

import com.ecommerce.application.dto.CreateProductRequest;
import com.ecommerce.application.dto.ProductResponse;
import com.ecommerce.application.dto.StockUpdateRequest;
import com.ecommerce.application.dto.UpdateProductRequest;
import com.ecommerce.domain.exception.InsufficientStockException;
import com.ecommerce.domain.exception.ProductNotFoundException;
import com.ecommerce.domain.model.Product;
import com.ecommerce.domain.model.ProductCategory;
import com.ecommerce.domain.port.out.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(CreateProductRequest request) {
        Product product = Product.builder()
            .name(request.name())
            .description(request.description())
            .price(request.price())
            .stock(request.stock())
            .category(request.category())
            .build();

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = getProductOrThrow(id);
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
            .map(ProductResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findByCategory(ProductCategory category) {
        return productRepository.findByCategory(category).stream()
            .map(ProductResponse::from)
            .toList();
    }

    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = getProductOrThrow(id);
        applyUpdates(product, request);
        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    public ProductResponse updateStock(Long id, StockUpdateRequest request) {
        Product product = getProductOrThrow(id);

        if (request.operation() == StockUpdateRequest.StockOperation.INCREMENT) {
            product.incrementStock(request.amount());
        } else {
            validateStockAvailability(product, request.amount());
            product.decrementStock(request.amount());
        }

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private void applyUpdates(Product product, UpdateProductRequest request) {
        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.stock() != null) product.setStock(request.stock());
        if (request.category() != null) product.setCategory(request.category());
    }

    private void validateStockAvailability(Product product, Integer amount) {
        if (product.getStock() < amount) {
            throw new InsufficientStockException(
                product.getId(),
                product.getStock(),
                amount
            );
        }
    }
}
