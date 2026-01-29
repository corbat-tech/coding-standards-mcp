package com.example.order.domain.exception;

/**
 * Exception thrown when event processing fails.
 */
public class EventProcessingException extends RuntimeException {

    private final String eventId;
    private final boolean retryable;

    public EventProcessingException(String eventId, String message, boolean retryable) {
        super(message);
        this.eventId = eventId;
        this.retryable = retryable;
    }

    public EventProcessingException(String eventId, String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.eventId = eventId;
        this.retryable = retryable;
    }

    public String getEventId() {
        return eventId;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
