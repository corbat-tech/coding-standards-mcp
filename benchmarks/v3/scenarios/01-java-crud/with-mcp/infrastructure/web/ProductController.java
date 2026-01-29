package com.example.products.infrastructure.web;

import com.example.products.application.CreateProductCommand;
import com.example.products.application.ProductService;
import com.example.products.application.UpdateProductCommand;
import com.example.products.domain.Product;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Product operations.
 * Infrastructure layer - handles HTTP concerns.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        CreateProductCommand command = request.toCommand();
        Product product = productService.createProduct(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ProductResponse.from(product));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        Product product = productService.getProduct(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String category) {
        List<Product> products = (category != null)
            ? productService.getProductsByCategory(category)
            : productService.getAllProducts();
        List<ProductResponse> response = products.stream()
            .map(ProductResponse::from)
            .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        UpdateProductCommand command = request.toCommand();
        Product product = productService.updateProduct(id, command);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
