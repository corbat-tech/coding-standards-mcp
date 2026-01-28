package com.example.order.domain.port;

import com.example.order.domain.model.ProcessedOrder;

import java.util.Optional;

public interface ProcessedOrderRepository {
    void save(ProcessedOrder order);
    Optional<ProcessedOrder> findByOrderId(String orderId);
    boolean existsByOrderId(String orderId);
}
