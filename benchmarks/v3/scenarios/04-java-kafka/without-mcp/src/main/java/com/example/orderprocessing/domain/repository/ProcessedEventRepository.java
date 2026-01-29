package com.example.orderprocessing.domain.repository;

import com.example.orderprocessing.domain.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * Check if an event has already been processed
     */
    boolean existsByEventId(String eventId);

    /**
     * Find processed events by consumer group
     */
    List<ProcessedEvent> findByConsumerGroup(String consumerGroup);

    /**
     * Find events processed before a certain time (for cleanup)
     */
    List<ProcessedEvent> findByProcessedAtBefore(Instant cutoffTime);

    /**
     * Delete old processed events (cleanup)
     */
    void deleteByProcessedAtBefore(Instant cutoffTime);
}
