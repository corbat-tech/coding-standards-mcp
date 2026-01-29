package com.example.orderprocessing.integration;

import com.example.orderprocessing.config.KafkaConfig;
import com.example.orderprocessing.domain.entity.InventoryItem;
import com.example.orderprocessing.domain.event.OrderCreatedEvent;
import com.example.orderprocessing.domain.repository.InventoryRepository;
import com.example.orderprocessing.domain.repository.ProcessedEventRepository;
import com.example.orderprocessing.producer.OrderEventProducer;
import com.example.orderprocessing.service.InventoryService;
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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Kafka message flow.
 * Tests the complete flow from producer to consumer with embedded Kafka.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
        topics = {KafkaConfig.ORDER_CREATED_TOPIC, KafkaConfig.ORDER_CREATED_DLT}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        // Clean up repositories
        processedEventRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Should publish OrderCreatedEvent to Kafka topic")
    void shouldPublishOrderCreatedEvent() {
        // Given
        OrderCreatedEvent event = createTestEvent();

        // When
        orderEventProducer.publishOrderCreatedEventSync(event);

        // Then - verify message is in topic
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group-" + UUID.randomUUID(), "false", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.orderprocessing.domain.event");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName());

        ConsumerFactory<String, OrderCreatedEvent> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, OrderCreatedEvent> consumer = cf.createConsumer();
        consumer.subscribe(Collections.singletonList(KafkaConfig.ORDER_CREATED_TOPIC));

        ConsumerRecords<String, OrderCreatedEvent> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records.count()).isGreaterThan(0);

        OrderCreatedEvent receivedEvent = records.iterator().next().value();
        assertThat(receivedEvent.getEventId()).isEqualTo(event.getEventId());
        assertThat(receivedEvent.getOrderId()).isEqualTo(event.getOrderId());

        consumer.close();
    }

    @Test
    @DisplayName("Should consume OrderCreatedEvent and update inventory")
    void shouldConsumeEventAndUpdateInventory() {
        // Given - Set up inventory
        String productId = "PROD-001";
        inventoryService.addOrUpdateInventory(productId, "Test Product", 100);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-001")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId(productId)
                                .productName("Test Product")
                                .quantity(5)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("50.00"))
                .build();

        // When
        orderEventProducer.publishOrderCreatedEventSync(event);

        // Then - Wait for consumer to process
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    InventoryItem item = inventoryRepository.findById(productId).orElseThrow();
                    assertThat(item.getQuantityReserved()).isEqualTo(5);
                    assertThat(item.getEffectiveAvailable()).isEqualTo(95);
                });

        // Verify event was marked as processed
        assertThat(processedEventRepository.existsByEventId(event.getEventId())).isTrue();
    }

    @Test
    @DisplayName("Should handle duplicate events idempotently")
    void shouldHandleDuplicateEventsIdempotently() {
        // Given - Set up inventory
        String productId = "PROD-002";
        inventoryService.addOrUpdateInventory(productId, "Test Product 2", 100);

        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-001")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId(productId)
                                .productName("Test Product 2")
                                .quantity(10)
                                .unitPrice(new BigDecimal("20.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("200.00"))
                .build();

        // When - Send the same event twice
        orderEventProducer.publishOrderCreatedEventSync(event);
        orderEventProducer.publishOrderCreatedEventSync(event);

        // Then - Stock should only be reserved once
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    InventoryItem item = inventoryRepository.findById(productId).orElseThrow();
                    // Should be 10, not 20 (idempotent processing)
                    assertThat(item.getQuantityReserved()).isEqualTo(10);
                    assertThat(item.getEffectiveAvailable()).isEqualTo(90);
                });
    }

    @Test
    @DisplayName("Should process multiple items in a single order")
    void shouldProcessMultipleItemsInOrder() {
        // Given - Set up inventory for multiple products
        inventoryService.addOrUpdateInventory("PROD-A", "Product A", 50);
        inventoryService.addOrUpdateInventory("PROD-B", "Product B", 30);
        inventoryService.addOrUpdateInventory("PROD-C", "Product C", 20);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-002")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId("PROD-A")
                                .productName("Product A")
                                .quantity(5)
                                .unitPrice(new BigDecimal("10.00"))
                                .build(),
                        OrderCreatedEvent.OrderItem.builder()
                                .productId("PROD-B")
                                .productName("Product B")
                                .quantity(3)
                                .unitPrice(new BigDecimal("15.00"))
                                .build(),
                        OrderCreatedEvent.OrderItem.builder()
                                .productId("PROD-C")
                                .productName("Product C")
                                .quantity(2)
                                .unitPrice(new BigDecimal("25.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("145.00"))
                .build();

        // When
        orderEventProducer.publishOrderCreatedEventSync(event);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(inventoryRepository.findById("PROD-A").orElseThrow().getQuantityReserved()).isEqualTo(5);
                    assertThat(inventoryRepository.findById("PROD-B").orElseThrow().getQuantityReserved()).isEqualTo(3);
                    assertThat(inventoryRepository.findById("PROD-C").orElseThrow().getQuantityReserved()).isEqualTo(2);
                });
    }

    private OrderCreatedEvent createTestEvent() {
        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-TEST")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId("PROD-TEST")
                                .productName("Test Product")
                                .quantity(1)
                                .unitPrice(new BigDecimal("99.99"))
                                .build()
                ))
                .totalAmount(new BigDecimal("99.99"))
                .build();
    }
}
