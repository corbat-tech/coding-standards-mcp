package com.example.kafka;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<ProcessedOrder, String> {
    Optional<ProcessedOrder> findByOrderId(String orderId);
    boolean existsByOrderId(String orderId);
}
