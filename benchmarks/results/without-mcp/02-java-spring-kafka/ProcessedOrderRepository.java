package com.orders.repository;

import com.orders.entity.ProcessedOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedOrderRepository extends JpaRepository<ProcessedOrder, String> {

    boolean existsByOrderId(String orderId);
}
