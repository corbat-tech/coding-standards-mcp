package com.example.product.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String category;

    protected Product() {}

    public Product(String name, String description, BigDecimal price, String category) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.description = description;
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }

    public void update(String name, String description, BigDecimal price, String category) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.description = description;
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
    }
}
