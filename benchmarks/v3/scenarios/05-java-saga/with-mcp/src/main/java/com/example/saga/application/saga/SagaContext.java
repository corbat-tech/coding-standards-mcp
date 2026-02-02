package com.example.saga.application.saga;

import com.example.saga.domain.entity.Order;
import java.util.HashMap;
import java.util.Map;

public class SagaContext {
    private final Order order;
    private final String shippingAddress;
    private final Map<String, Object> data = new HashMap<>();

    public SagaContext(Order order, String shippingAddress) {
        this.order = order;
        this.shippingAddress = shippingAddress;
    }

    public Order getOrder() { return order; }
    public String getShippingAddress() { return shippingAddress; }

    public void put(String key, Object value) { data.put(key, value); }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) { return (T) data.get(key); }

    public boolean has(String key) { return data.containsKey(key); }
}
