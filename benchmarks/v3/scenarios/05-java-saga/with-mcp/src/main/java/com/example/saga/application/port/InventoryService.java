package com.example.saga.application.port;

import com.example.saga.domain.entity.OrderItem;
import com.example.saga.domain.valueobject.OrderId;
import java.util.List;

public interface InventoryService {
    void reserveInventory(OrderId orderId, List<OrderItem> items);
    void releaseInventory(OrderId orderId, List<OrderItem> items);
}
