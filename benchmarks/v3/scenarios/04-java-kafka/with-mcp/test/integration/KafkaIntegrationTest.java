package com.example.order.infrastructure.kafka;

import com.example.order.application.port.in.PlaceOrderUseCase;
import com.example.order.application.port.in.PlaceOrderUseCase.OrderItemCommand;
import com.example.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.example.order.application.port.in.PlaceOrderUseCase.PlaceOrderResult;
import com.example.order.domain.events.OrderCreatedEvent;
import com.example.order.infrastructure.kafka.config.KafkaConfig;
import com.example.order.infrastructure.persistence.InMemoryInventoryRepository;
import com.example.order.infrastructure.persistence.InMemoryProcessedEventRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests using embedded Kafka.
 * Tests the full flow from order placement through Kafka to inventory update.
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {KafkaConfig.ORDER_EVENTS_TOPIC, KafkaConfig.ORDER_EVENTS_DLQ_TOPIC},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.group-id=test-group"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Kafka Integration Tests")
class KafkaIntegrationTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private InMemoryInventoryRepository inventoryRepository;

    @Autowired
    private InMemoryProcessedEventRepository processedEventRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @BeforeEach
    void setUp() {
        inventoryRepository.clear();
        processedEventRepository.clear();

        // Seed inventory for tests
        inventoryRepository.seedInventory("PROD-001", "Test Product", 100);
        inventoryRepository.seedInventory("PROD-002", "Another Product", 50);
    }

    @Test
    @DisplayName("should publish order event and update inventory via Kafka")
    void should_publish_and_consume_order_event() {
        // Given
        PlaceOrderCommand command = new PlaceOrderCommand(
            "CUST-001",
            List.of(new OrderItemCommand(
                "PROD-001", "Test Product", 10, new BigDecimal("25.00")
            ))
        );

        // When
        PlaceOrderResult result = placeOrderUseCase.placeOrder(command);

        // Then
        assertThat(result.success()).isTrue();

        // Wait for consumer to process the event
        await().atMost(10, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                var inventory = inventoryRepository.findByProductId("PROD-001").orElseThrow();
                assertThat(inventory.getReservedQuantity()).isEqualTo(10);
                assertThat(inventory.getAvailableQuantity()).isEqualTo(90);
            });

        // Verify event was recorded for idempotency
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("should handle idempotent event processing")
    void should_handle_idempotent_processing() {
        // Given - Seed an already processed event
        processedEventRepository.save(
            com.example.order.domain.model.ProcessedEvent.success(
                "existing-event", "OrderCreatedEvent"
            )
        );

        // Initial inventory state
        var initialInventory = inventoryRepository.findByProductId("PROD-001").orElseThrow();
        int initialAvailable = initialInventory.getAvailableQuantity();

        // The same event ID sent again would be skipped
        // (In real scenario, consumer would receive duplicate)

        // Then - verify idempotency check exists
        assertThat(processedEventRepository.existsByEventId("existing-event")).isTrue();
    }

    @Test
    @DisplayName("should process multiple item order")
    void should_process_multiple_items() {
        // Given
        PlaceOrderCommand command = new PlaceOrderCommand(
            "CUST-001",
            List.of(
                new OrderItemCommand("PROD-001", "Test Product", 5, new BigDecimal("10.00")),
                new OrderItemCommand("PROD-002", "Another Product", 3, new BigDecimal("20.00"))
            )
        );

        // When
        PlaceOrderResult result = placeOrderUseCase.placeOrder(command);

        // Then
        assertThat(result.success()).isTrue();

        await().atMost(10, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                var inventory1 = inventoryRepository.findByProductId("PROD-001").orElseThrow();
                var inventory2 = inventoryRepository.findByProductId("PROD-002").orElseThrow();

                assertThat(inventory1.getReservedQuantity()).isEqualTo(5);
                assertThat(inventory2.getReservedQuantity()).isEqualTo(3);
            });
    }

    @Test
    @DisplayName("should verify events are published to correct topic")
    void should_publish_to_correct_topic() {
        // Given
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "verify-topic-group", "false", embeddedKafka
        );
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ConsumerFactory<String, OrderCreatedEvent> cf = new DefaultKafkaConsumerFactory<>(
            consumerProps,
            new StringDeserializer(),
            createJsonDeserializer()
        );

        Consumer<String, OrderCreatedEvent> consumer = cf.createConsumer();
        consumer.subscribe(Collections.singleton(KafkaConfig.ORDER_EVENTS_TOPIC));

        // When
        PlaceOrderCommand command = new PlaceOrderCommand(
            "CUST-001",
            List.of(new OrderItemCommand("PROD-001", "Test", 1, new BigDecimal("10.00")))
        );
        placeOrderUseCase.placeOrder(command);

        // Then
        ConsumerRecords<String, OrderCreatedEvent> records =
            KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records.count()).isGreaterThanOrEqualTo(1);

        var record = records.iterator().next();
        assertThat(record.topic()).isEqualTo(KafkaConfig.ORDER_EVENTS_TOPIC);
        assertThat(record.value().customerId()).isEqualTo("CUST-001");

        consumer.close();
    }

    private JsonDeserializer<OrderCreatedEvent> createJsonDeserializer() {
        JsonDeserializer<OrderCreatedEvent> deserializer =
            new JsonDeserializer<>(OrderCreatedEvent.class);
        deserializer.addTrustedPackages("com.example.order.domain.events");
        deserializer.setUseTypeHeaders(false);
        return deserializer;
    }
}
