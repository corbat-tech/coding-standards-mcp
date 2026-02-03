package com.example.saga.domain.entity;

import com.example.saga.domain.valueobject.Money;

public record OrderItem(String productId, int quantity, Money price) {}
