package com.example.order.infrastructure.persistence;

import com.example.order.application.port.out.ProcessedEventRepository;
import com.example.order.domain.model.ProcessedEvent;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of ProcessedEventRepository for testing.
 * Provides idempotency tracking.
 */
@Repository
public class InMemoryProcessedEventRepository implements ProcessedEventRepository {

    private final Map<String, ProcessedEvent> storage = new ConcurrentHashMap<>();

    @Override
    public boolean existsByEventId(String eventId) {
        return storage.containsKey(eventId);
    }

    @Override
    public Optional<ProcessedEvent> findByEventId(String eventId) {
        return Optional.ofNullable(storage.get(eventId));
    }

    @Override
    public ProcessedEvent save(ProcessedEvent processedEvent) {
        storage.put(processedEvent.eventId(), processedEvent);
        return processedEvent;
    }

    /**
     * Clears all processed event data. Useful for test cleanup.
     */
    public void clear() {
        storage.clear();
    }

    /**
     * Returns the count of processed events.
     *
     * @return the number of processed events
     */
    public int count() {
        return storage.size();
    }
}
