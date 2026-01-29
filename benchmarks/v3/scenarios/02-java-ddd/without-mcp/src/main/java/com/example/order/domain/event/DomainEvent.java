package com.example.order.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events.
 * Domain events represent something significant that happened in the domain.
 */
public interface DomainEvent {

    /**
     * Returns the unique identifier of this event.
     *
     * @return the event ID
     */
    UUID getEventId();

    /**
     * Returns the timestamp when this event occurred.
     *
     * @return the event timestamp
     */
    Instant getOccurredOn();

    /**
     * Returns the type name of this event.
     *
     * @return the event type name
     */
    String getEventType();
}
