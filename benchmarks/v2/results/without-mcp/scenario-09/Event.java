package com.example.eventsourcing;

import java.time.Instant;
import java.util.UUID;

public abstract class Event {
    private final String eventId;
    private final String aggregateId;
    private final Instant timestamp;
    private final int version;

    protected Event(String aggregateId, int version) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.timestamp = Instant.now();
        this.version = version;
    }

    public String getEventId() { return eventId; }
    public String getAggregateId() { return aggregateId; }
    public Instant getTimestamp() { return timestamp; }
    public int getVersion() { return version; }
    public abstract String getEventType();
}
