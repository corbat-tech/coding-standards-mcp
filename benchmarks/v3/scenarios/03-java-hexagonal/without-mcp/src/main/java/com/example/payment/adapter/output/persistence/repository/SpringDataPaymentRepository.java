package com.example.payment.adapter.output.persistence.repository;

import com.example.payment.adapter.output.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for Payment entities.
 */
@Repository
public interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    List<PaymentJpaEntity> findByCustomerId(String customerId);

    List<PaymentJpaEntity> findByOrderId(String orderId);
}
