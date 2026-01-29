package application.port;

import domain.event.DomainEvent;

import java.util.List;

/**
 * Port interface for publishing domain events.
 * Infrastructure adapters will implement this for actual event dispatch.
 */
public interface DomainEventPublisher {

    /**
     * Publishes a single domain event.
     * @param event the event to publish
     */
    void publish(DomainEvent event);

    /**
     * Publishes multiple domain events.
     * @param events the events to publish
     */
    void publishAll(List<DomainEvent> events);
}
