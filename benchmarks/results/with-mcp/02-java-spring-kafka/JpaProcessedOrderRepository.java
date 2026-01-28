package com.orders.infrastructure.persistence;

import com.orders.domain.model.ProcessedOrder;
import com.orders.domain.port.out.ProcessedOrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProcessedOrderRepository
    extends JpaRepository<ProcessedOrder, String>, ProcessedOrderRepository {
}
