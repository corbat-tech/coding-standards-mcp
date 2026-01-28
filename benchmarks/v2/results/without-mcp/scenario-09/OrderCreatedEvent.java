package com.example.eventsourcing;

public class OrderCreatedEvent extends Event {
    private final String customerId;

    public OrderCreatedEvent(String orderId, String customerId, int version) {
        super(orderId, version);
        this.customerId = customerId;
    }

    public String getCustomerId() { return customerId; }

    @Override
    public String getEventType() {
        return "OrderCreated";
    }
}
