package com.example.eventsourcing;

import java.util.List;

public interface EventStore {
    void append(Event event);
    List<Event> getEvents(String aggregateId);
    List<Event> getAllEvents();
}
