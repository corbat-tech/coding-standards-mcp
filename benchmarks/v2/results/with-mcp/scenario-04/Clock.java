package com.example.order.domain.port;

import java.time.Instant;

public interface Clock {
    Instant now();
}
