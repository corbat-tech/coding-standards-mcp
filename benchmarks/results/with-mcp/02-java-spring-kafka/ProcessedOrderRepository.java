package com.orders.domain.port.out;

import com.orders.domain.model.ProcessedOrder;
import java.util.Optional;

public interface ProcessedOrderRepository {

    ProcessedOrder save(ProcessedOrder order);

    Optional<ProcessedOrder> findById(String orderId);

    boolean existsById(String orderId);
}
