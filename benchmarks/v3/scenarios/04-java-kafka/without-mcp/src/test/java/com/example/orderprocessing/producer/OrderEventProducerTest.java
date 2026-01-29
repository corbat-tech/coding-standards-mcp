package com.example.orderprocessing.producer;

import com.example.orderprocessing.config.KafkaConfig;
import com.example.orderprocessing.domain.event.OrderCreatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<OrderCreatedEvent> eventCaptor;

    private OrderEventProducer orderEventProducer;

    @BeforeEach
    void setUp() {
        orderEventProducer = new OrderEventProducer(kafkaTemplate);
    }

    @Test
    @DisplayName("Should publish OrderCreatedEvent to correct topic with order ID as key")
    void shouldPublishEventToCorrectTopic() {
        // Given
        OrderCreatedEvent event = createTestEvent();

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = new CompletableFuture<>();
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(KafkaConfig.ORDER_CREATED_TOPIC, 0),
                0L, 0, System.currentTimeMillis(), 0, 0);
        SendResult<String, OrderCreatedEvent> sendResult = new SendResult<>(
                new ProducerRecord<>(KafkaConfig.ORDER_CREATED_TOPIC, event.getOrderId(), event),
                metadata);
        future.complete(sendResult);

        when(kafkaTemplate.send(any(String.class), any(String.class), any(OrderCreatedEvent.class)))
                .thenReturn(future);

        // When
        CompletableFuture<SendResult<String, OrderCreatedEvent>> result =
                orderEventProducer.publishOrderCreatedEvent(event);

        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.ORDER_CREATED_TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo(event.getOrderId());
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    @DisplayName("Should publish event synchronously")
    void shouldPublishEventSynchronously() {
        // Given
        OrderCreatedEvent event = createTestEvent();

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = new CompletableFuture<>();
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(KafkaConfig.ORDER_CREATED_TOPIC, 0),
                0L, 0, System.currentTimeMillis(), 0, 0);
        SendResult<String, OrderCreatedEvent> sendResult = new SendResult<>(
                new ProducerRecord<>(KafkaConfig.ORDER_CREATED_TOPIC, event.getOrderId(), event),
                metadata);
        future.complete(sendResult);

        when(kafkaTemplate.send(any(String.class), any(String.class), any(OrderCreatedEvent.class)))
                .thenReturn(future);

        // When
        SendResult<String, OrderCreatedEvent> result = orderEventProducer.publishOrderCreatedEventSync(event);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecordMetadata().topic()).isEqualTo(KafkaConfig.ORDER_CREATED_TOPIC);
    }

    @Test
    @DisplayName("Should throw exception when synchronous publish fails")
    void shouldThrowExceptionWhenSyncPublishFails() {
        // Given
        OrderCreatedEvent event = createTestEvent();

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka connection failed"));

        when(kafkaTemplate.send(any(String.class), any(String.class), any(OrderCreatedEvent.class)))
                .thenReturn(future);

        // When/Then
        assertThatThrownBy(() -> orderEventProducer.publishOrderCreatedEventSync(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event");
    }

    @Test
    @DisplayName("Should use orderId as partition key for message ordering")
    void shouldUseOrderIdAsPartitionKey() {
        // Given
        String orderId = "ORDER-12345";
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId("CUST-001")
                .items(List.of())
                .totalAmount(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .build();

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));

        when(kafkaTemplate.send(any(String.class), any(String.class), any(OrderCreatedEvent.class)))
                .thenReturn(future);

        // When
        orderEventProducer.publishOrderCreatedEvent(event);

        // Then
        verify(kafkaTemplate).send(eq(KafkaConfig.ORDER_CREATED_TOPIC), eq(orderId), eq(event));
    }

    private OrderCreatedEvent createTestEvent() {
        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-TEST")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId("PROD-001")
                                .productName("Test Product")
                                .quantity(2)
                                .unitPrice(new BigDecimal("49.99"))
                                .build()
                ))
                .totalAmount(new BigDecimal("99.98"))
                .createdAt(Instant.now())
                .build();
    }
}
