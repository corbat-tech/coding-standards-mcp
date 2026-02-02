package com.example.payment.infrastructure.adapter.persistence;

import com.example.payment.application.port.output.PaymentRepository;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.valueobject.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final JpaPaymentRepository jpaRepository;

    public PaymentRepositoryAdapter(JpaPaymentRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = toEntity(payment);
        jpaRepository.save(entity);
        return payment;
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return jpaRepository.findById(id.toString()).map(this::toDomain);
    }

    private PaymentEntity toEntity(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(payment.getId().toString());
        entity.setOrderId(payment.getOrderId());
        entity.setAmount(payment.getAmount().getAmount());
        entity.setCurrency(payment.getAmount().getCurrency().getCurrencyCode());
        entity.setStatus(payment.getStatus().name());
        entity.setGatewayTransactionId(payment.getGatewayTransactionId());
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setUpdatedAt(payment.getUpdatedAt());
        return entity;
    }

    private Payment toDomain(PaymentEntity entity) {
        Payment payment = new Payment(
                PaymentId.from(entity.getId()),
                entity.getOrderId(),
                Money.of(entity.getAmount(), entity.getCurrency())
        );
        return payment;
    }
}
