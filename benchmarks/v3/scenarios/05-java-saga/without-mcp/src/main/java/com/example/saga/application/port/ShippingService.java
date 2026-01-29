package com.example.saga.application.port;

/**
 * Port interface for shipping operations.
 */
public interface ShippingService {

    /**
     * Creates a shipment for an order.
     *
     * @param orderId    the order ID
     * @param customerId the customer ID
     * @return the tracking ID
     */
    String createShipment(String orderId, String customerId);

    /**
     * Cancels a shipment.
     *
     * @param trackingId the tracking ID to cancel
     */
    void cancelShipment(String trackingId);
}
