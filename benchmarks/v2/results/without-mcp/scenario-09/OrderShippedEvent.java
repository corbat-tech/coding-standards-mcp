package com.example.eventsourcing;

public class OrderShippedEvent extends Event {
    private final String shippingAddress;
    private final String trackingNumber;

    public OrderShippedEvent(String orderId, String shippingAddress, String trackingNumber, int version) {
        super(orderId, version);
        this.shippingAddress = shippingAddress;
        this.trackingNumber = trackingNumber;
    }

    public String getShippingAddress() { return shippingAddress; }
    public String getTrackingNumber() { return trackingNumber; }

    @Override
    public String getEventType() {
        return "OrderShipped";
    }
}
