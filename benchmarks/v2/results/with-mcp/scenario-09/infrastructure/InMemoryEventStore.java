package com.example.order.infrastructure;

import com.example.order.domain.event.DomainEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryEventStore implements EventStore {
    private final Map<String, List<DomainEvent>> store = new ConcurrentHashMap<>();

    @Override
    public void append(String aggregateId, List<DomainEvent> events, int expectedVersion) {
        store.compute(aggregateId, (key, existing) -> {
            List<DomainEvent> current = existing != null ? existing : new ArrayList<>();

            int currentVersion = current.isEmpty() ? 0 :
                current.get(current.size() - 1).getVersion();

            if (currentVersion != expectedVersion) {
                throw new ConcurrencyException(
                    String.format("Expected version %d but found %d",
                        expectedVersion, currentVersion)
                );
            }

            List<DomainEvent> updated = new ArrayList<>(current);
            updated.addAll(events);
            return updated;
        });
    }

    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        return store.getOrDefault(aggregateId, Collections.emptyList());
    }

    @Override
    public List<DomainEvent> getEvents(String aggregateId, int fromVersion) {
        return store.getOrDefault(aggregateId, Collections.emptyList())
            .stream()
            .filter(e -> e.getVersion() > fromVersion)
            .collect(Collectors.toList());
    }

    public void clear() {
        store.clear();
    }
}
