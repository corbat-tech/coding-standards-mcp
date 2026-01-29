package com.example.order.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity for tracking processed events to ensure idempotency.
 */
public record ProcessedEvent(
    String eventId,
    String eventType,
    Instant processedAt,
    boolean success,
    String errorMessage
) {
    public ProcessedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(processedAt, "processedAt must not be null");
    }

    public static ProcessedEvent success(String eventId, String eventType) {
        return new ProcessedEvent(eventId, eventType, Instant.now(), true, null);
    }

    public static ProcessedEvent failure(String eventId, String eventType, String errorMessage) {
        return new ProcessedEvent(eventId, eventType, Instant.now(), false, errorMessage);
    }
}
