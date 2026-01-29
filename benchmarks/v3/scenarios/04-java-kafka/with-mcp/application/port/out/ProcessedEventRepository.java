package com.example.order.application.port.out;

import com.example.order.domain.model.ProcessedEvent;

import java.util.Optional;

/**
 * Output port for tracking processed events (idempotency).
 * Implemented by infrastructure adapters.
 */
public interface ProcessedEventRepository {

    /**
     * Checks if an event has already been processed.
     *
     * @param eventId the event ID to check
     * @return true if the event was already processed
     */
    boolean existsByEventId(String eventId);

    /**
     * Finds a processed event by its ID.
     *
     * @param eventId the event ID to search for
     * @return an Optional containing the processed event if found
     */
    Optional<ProcessedEvent> findByEventId(String eventId);

    /**
     * Records a processed event.
     *
     * @param processedEvent the event processing record to save
     * @return the saved record
     */
    ProcessedEvent save(ProcessedEvent processedEvent);
}
