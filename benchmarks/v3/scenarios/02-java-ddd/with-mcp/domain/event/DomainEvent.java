package domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events.
 */
public interface DomainEvent {

    /**
     * Unique identifier for this event instance.
     */
    UUID getEventId();

    /**
     * Timestamp when the event occurred.
     */
    Instant getOccurredAt();

    /**
     * Name/type of the event for routing purposes.
     */
    String getEventType();
}
