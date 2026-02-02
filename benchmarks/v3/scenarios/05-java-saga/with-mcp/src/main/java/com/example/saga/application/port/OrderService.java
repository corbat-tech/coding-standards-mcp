package com.example.saga.application.port;

import com.example.saga.domain.entity.Order;
import com.example.saga.domain.valueobject.OrderId;

public interface OrderService {
    Order createOrder(Order order);
    void cancelOrder(OrderId orderId);
}
