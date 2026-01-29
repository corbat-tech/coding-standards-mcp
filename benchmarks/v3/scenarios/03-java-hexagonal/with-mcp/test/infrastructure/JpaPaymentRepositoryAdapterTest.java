package com.payment.infrastructure.adapter.out.persistence;

import com.payment.domain.entity.Payment;
import com.payment.domain.entity.PaymentStatus;
import com.payment.domain.valueobject.Money;
import com.payment.domain.valueobject.PaymentId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaPaymentRepositoryAdapter.class)
@DisplayName("JPA Payment Repository Adapter")
class JpaPaymentRepositoryAdapterTest {

    @Autowired
    private JpaPaymentRepositoryAdapter repository;

    @Autowired
    private SpringDataPaymentRepository springDataRepository;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        springDataRepository.deleteAll();
        testPayment = Payment.create(
            "order-123",
            "customer-456",
            Money.usd(new BigDecimal("100.00"))
        );
    }

    @Test
    @DisplayName("should save and retrieve payment by ID")
    void shouldSaveAndRetrievePaymentById() {
        // When
        Payment saved = repository.save(testPayment);
        Optional<Payment> found = repository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo("order-123");
        assertThat(found.get().getCustomerId()).isEqualTo("customer-456");
        assertThat(found.get().getAmount().getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("should find payment by order ID")
    void shouldFindPaymentByOrderId() {
        // Given
        repository.save(testPayment);

        // When
        Optional<Payment> found = repository.findByOrderId("order-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo("order-123");
    }

    @Test
    @DisplayName("should return empty when payment not found")
    void shouldReturnEmptyWhenPaymentNotFound() {
        // When
        Optional<Payment> found = repository.findById(PaymentId.generate());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should check if payment exists by order ID")
    void shouldCheckIfPaymentExistsByOrderId() {
        // Given
        repository.save(testPayment);

        // Then
        assertThat(repository.existsByOrderId("order-123")).isTrue();
        assertThat(repository.existsByOrderId("order-999")).isFalse();
    }

    @Test
    @DisplayName("should persist payment status changes")
    void shouldPersistPaymentStatusChanges() {
        // Given
        Payment saved = repository.save(testPayment);
        saved.markAsProcessing();
        saved.markAsCompleted("txn_123");

        // When
        repository.save(saved);
        Optional<Payment> found = repository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(found.get().getGatewayTransactionId()).isEqualTo("txn_123");
    }

    @Test
    @DisplayName("should persist refund information")
    void shouldPersistRefundInformation() {
        // Given
        Payment saved = repository.save(testPayment);
        saved.markAsProcessing();
        saved.markAsCompleted("txn_123");
        saved.refund(Money.usd(new BigDecimal("50.00")));

        // When
        repository.save(saved);
        Optional<Payment> found = repository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(found.get().getRefundedAmount().getAmount()).isEqualByComparingTo("50.00");
    }
}
