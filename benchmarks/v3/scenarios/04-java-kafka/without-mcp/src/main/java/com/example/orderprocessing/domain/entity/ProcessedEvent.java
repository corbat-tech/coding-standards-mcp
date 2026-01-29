package com.example.orderprocessing.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity to track processed events for idempotency.
 * Each event ID is stored once it's been successfully processed,
 * preventing duplicate processing of the same event.
 */
@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_processed_events_event_id", columnList = "event_id", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "consumer_group", nullable = false)
    private String consumerGroup;

    public static ProcessedEvent create(String eventId, String eventType, String consumerGroup) {
        return ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .consumerGroup(consumerGroup)
                .processedAt(Instant.now())
                .build();
    }
}
