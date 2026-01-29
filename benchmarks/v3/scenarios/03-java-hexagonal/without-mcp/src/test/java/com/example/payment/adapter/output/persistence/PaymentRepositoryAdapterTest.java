package com.example.payment.adapter.output.persistence;

import com.example.payment.adapter.output.persistence.entity.PaymentJpaEntity;
import com.example.payment.adapter.output.persistence.mapper.PaymentMapper;
import com.example.payment.adapter.output.persistence.repository.SpringDataPaymentRepository;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.valueobject.Money;
import com.example.payment.domain.valueobject.PaymentId;
import com.example.payment.domain.valueobject.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentRepositoryAdapter.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRepositoryAdapter")
class PaymentRepositoryAdapterTest {

    @Mock
    private SpringDataPaymentRepository springDataRepository;

    @Mock
    private PaymentMapper mapper;

    private PaymentRepositoryAdapter adapter;

    private Payment domainPayment;
    private PaymentJpaEntity jpaEntity;
    private UUID paymentUuid;

    @BeforeEach
    void setUp() {
        adapter = new PaymentRepositoryAdapter(springDataRepository, mapper);

        paymentUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        domainPayment = Payment.builder()
                .id(PaymentId.of(paymentUuid))
                .orderId("order-123")
                .customerId("customer-456")
                .amount(Money.of(new BigDecimal("100.00"), "USD"))
                .status(PaymentStatus.COMPLETED)
                .refundedAmount(Money.zero("USD"))
                .gatewayTransactionId("ch_123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        jpaEntity = new PaymentJpaEntity();
        jpaEntity.setId(paymentUuid);
        jpaEntity.setOrderId("order-123");
        jpaEntity.setCustomerId("customer-456");
        jpaEntity.setAmount(new BigDecimal("100.00"));
        jpaEntity.setCurrency("USD");
        jpaEntity.setStatus(PaymentStatus.COMPLETED);
        jpaEntity.setRefundedAmount(BigDecimal.ZERO);
        jpaEntity.setGatewayTransactionId("ch_123");
        jpaEntity.setCreatedAt(LocalDateTime.now());
        jpaEntity.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("should save payment")
    void shouldSavePayment() {
        when(mapper.toJpaEntity(any(Payment.class))).thenReturn(jpaEntity);
        when(springDataRepository.save(any(PaymentJpaEntity.class))).thenReturn(jpaEntity);
        when(mapper.toDomain(any(PaymentJpaEntity.class))).thenReturn(domainPayment);

        Payment result = adapter.save(domainPayment);

        assertNotNull(result);
        assertEquals(domainPayment.getId(), result.getId());
        verify(springDataRepository).save(jpaEntity);
    }

    @Test
    @DisplayName("should find payment by ID")
    void shouldFindPaymentById() {
        when(springDataRepository.findById(paymentUuid)).thenReturn(Optional.of(jpaEntity));
        when(mapper.toDomain(jpaEntity)).thenReturn(domainPayment);

        Optional<Payment> result = adapter.findById(PaymentId.of(paymentUuid));

        assertTrue(result.isPresent());
        assertEquals(domainPayment.getId(), result.get().getId());
    }

    @Test
    @DisplayName("should return empty when payment not found")
    void shouldReturnEmptyWhenPaymentNotFound() {
        when(springDataRepository.findById(paymentUuid)).thenReturn(Optional.empty());

        Optional<Payment> result = adapter.findById(PaymentId.of(paymentUuid));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should find payments by customer ID")
    void shouldFindPaymentsByCustomerId() {
        when(springDataRepository.findByCustomerId("customer-456")).thenReturn(List.of(jpaEntity));
        when(mapper.toDomain(jpaEntity)).thenReturn(domainPayment);

        List<Payment> result = adapter.findByCustomerId("customer-456");

        assertEquals(1, result.size());
        assertEquals("customer-456", result.get(0).getCustomerId());
    }

    @Test
    @DisplayName("should find payments by order ID")
    void shouldFindPaymentsByOrderId() {
        when(springDataRepository.findByOrderId("order-123")).thenReturn(List.of(jpaEntity));
        when(mapper.toDomain(jpaEntity)).thenReturn(domainPayment);

        List<Payment> result = adapter.findByOrderId("order-123");

        assertEquals(1, result.size());
        assertEquals("order-123", result.get(0).getOrderId());
    }

    @Test
    @DisplayName("should check if payment exists")
    void shouldCheckIfPaymentExists() {
        when(springDataRepository.existsById(paymentUuid)).thenReturn(true);

        boolean result = adapter.existsById(PaymentId.of(paymentUuid));

        assertTrue(result);
    }

    @Test
    @DisplayName("should delete payment by ID")
    void shouldDeletePaymentById() {
        adapter.deleteById(PaymentId.of(paymentUuid));

        verify(springDataRepository).deleteById(paymentUuid);
    }
}
