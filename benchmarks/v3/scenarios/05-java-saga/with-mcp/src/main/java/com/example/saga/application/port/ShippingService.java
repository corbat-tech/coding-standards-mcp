package com.example.saga.application.port;

import com.example.saga.domain.valueobject.OrderId;

public interface ShippingService {
    String createShipment(OrderId orderId, String address);
    void cancelShipment(String shipmentId);
}
