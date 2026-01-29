package com.example.order.application.port.in;

import com.example.order.domain.events.OrderCreatedEvent;

/**
 * Input port for processing order events from Kafka.
 * Defines the contract for the inventory update use case.
 */
public interface ProcessOrderEventUseCase {

    /**
     * Processes an OrderCreatedEvent to update inventory.
     *
     * @param event the order event to process
     * @return the result of the processing
     */
    ProcessingResult processOrderCreated(OrderCreatedEvent event);

    /**
     * Result of event processing.
     */
    record ProcessingResult(
        String eventId,
        boolean success,
        boolean skipped,
        String message
    ) {
        public static ProcessingResult success(String eventId) {
            return new ProcessingResult(eventId, true, false, "Event processed successfully");
        }

        public static ProcessingResult skippedDuplicate(String eventId) {
            return new ProcessingResult(eventId, true, true, "Event already processed (idempotent skip)");
        }

        public static ProcessingResult failure(String eventId, String message) {
            return new ProcessingResult(eventId, false, false, message);
        }
    }
}
