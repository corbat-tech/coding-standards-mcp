package com.example.order.domain.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredOn();
}
