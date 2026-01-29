package com.example.orderprocessing.integration;

import com.example.orderprocessing.config.KafkaConfig;
import com.example.orderprocessing.domain.event.OrderCreatedEvent;
import com.example.orderprocessing.domain.repository.InventoryRepository;
import com.example.orderprocessing.domain.repository.ProcessedEventRepository;
import com.example.orderprocessing.producer.OrderEventProducer;
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
 * Integration tests for Dead Letter Queue functionality.
 * Tests that failed messages are properly routed to the DLT.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
        topics = {KafkaConfig.ORDER_CREATED_TOPIC, KafkaConfig.ORDER_CREATED_DLT}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DeadLetterQueueIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Should send message to DLT when product not found")
    void shouldSendToDltWhenProductNotFound() {
        // Given - No inventory set up for the product
        String nonExistentProductId = "NON-EXISTENT-" + UUID.randomUUID();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-001")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId(nonExistentProductId)
                                .productName("Non-existent Product")
                                .quantity(5)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("50.00"))
                .build();

        // When
        orderEventProducer.publishOrderCreatedEventSync(event);

        // Then - Message should appear in DLT after retries
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "dlt-test-group-" + UUID.randomUUID(), "false", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.orderprocessing.domain.event");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName());

        ConsumerFactory<String, OrderCreatedEvent> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, OrderCreatedEvent> consumer = cf.createConsumer();
        consumer.subscribe(Collections.singletonList(KafkaConfig.ORDER_CREATED_DLT));

        // Wait for message to arrive in DLT (after retries)
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ConsumerRecords<String, OrderCreatedEvent> records =
                            KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
                    assertThat(records.count()).isGreaterThan(0);

                    OrderCreatedEvent dltEvent = records.iterator().next().value();
                    assertThat(dltEvent.getEventId()).isEqualTo(event.getEventId());
                });

        consumer.close();
    }

    @Test
    @DisplayName("Should send message to DLT when insufficient stock after retries")
    void shouldSendToDltWhenInsufficientStock() {
        // Given - Set up inventory with limited stock
        String productId = "LIMITED-STOCK-" + UUID.randomUUID();
        inventoryRepository.save(
                com.example.orderprocessing.domain.entity.InventoryItem.builder()
                        .productId(productId)
                        .productName("Limited Stock Product")
                        .quantityAvailable(5)
                        .quantityReserved(0)
                        .build()
        );

        // Request more than available
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-001")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId(productId)
                                .productName("Limited Stock Product")
                                .quantity(100) // More than available
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("1000.00"))
                .build();

        // When
        orderEventProducer.publishOrderCreatedEventSync(event);

        // Then - Message should appear in DLT after retries
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "dlt-insufficient-test-" + UUID.randomUUID(), "false", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.orderprocessing.domain.event");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName());

        ConsumerFactory<String, OrderCreatedEvent> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, OrderCreatedEvent> consumer = cf.createConsumer();
        consumer.subscribe(Collections.singletonList(KafkaConfig.ORDER_CREATED_DLT));

        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ConsumerRecords<String, OrderCreatedEvent> records =
                            KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
                    assertThat(records.count()).isGreaterThan(0);
                });

        consumer.close();
    }
}
