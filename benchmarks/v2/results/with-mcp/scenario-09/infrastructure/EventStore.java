package com.example.order.infrastructure;

import com.example.order.domain.event.DomainEvent;

import java.util.List;

public interface EventStore {
    void append(String aggregateId, List<DomainEvent> events, int expectedVersion);
    List<DomainEvent> getEvents(String aggregateId);
    List<DomainEvent> getEvents(String aggregateId, int fromVersion);
}
