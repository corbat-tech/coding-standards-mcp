package com.example.eventsourcing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryEventStore implements EventStore {
    private final List<Event> events = new ArrayList<>();

    @Override
    public synchronized void append(Event event) {
        events.add(event);
    }

    @Override
    public synchronized List<Event> getEvents(String aggregateId) {
        return events.stream()
            .filter(e -> e.getAggregateId().equals(aggregateId))
            .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Event> getAllEvents() {
        return new ArrayList<>(events);
    }
}
