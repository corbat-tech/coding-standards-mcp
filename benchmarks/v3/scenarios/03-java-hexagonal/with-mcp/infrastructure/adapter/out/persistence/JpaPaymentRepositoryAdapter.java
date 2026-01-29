package com.payment.infrastructure.adapter.out.persistence;

import com.payment.domain.entity.Payment;
import com.payment.domain.port.output.PaymentRepository;
import com.payment.domain.valueobject.PaymentId;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA adapter implementing the PaymentRepository port.
 * Secondary/driven adapter for persistence.
 */
@Component
public class JpaPaymentRepositoryAdapter implements PaymentRepository {

    private final SpringDataPaymentRepository springDataRepository;

    public JpaPaymentRepositoryAdapter(SpringDataPaymentRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = PaymentJpaEntity.fromDomain(payment);
        PaymentJpaEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return springDataRepository.findById(id.getValue())
            .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        return springDataRepository.findByOrderId(orderId)
            .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public boolean existsByOrderId(String orderId) {
        return springDataRepository.existsByOrderId(orderId);
    }
}
