package com.example.orderprocessing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
        topics = {"order-created", "order-created.DLT"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderProcessingApplicationTests {

    @Test
    void contextLoads() {
        // Verify that the application context loads successfully
    }
}
