package com.example.payment.adapter.output.persistence;

import com.example.payment.adapter.output.persistence.entity.PaymentJpaEntity;
import com.example.payment.adapter.output.persistence.mapper.PaymentMapper;
import com.example.payment.adapter.output.persistence.repository.SpringDataPaymentRepository;
import com.example.payment.application.port.output.PaymentRepository;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.valueobject.PaymentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing PaymentRepository port using JPA.
 */
@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(PaymentRepositoryAdapter.class);

    private final SpringDataPaymentRepository springDataRepository;
    private final PaymentMapper mapper;

    public PaymentRepositoryAdapter(SpringDataPaymentRepository springDataRepository,
                                    PaymentMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Payment save(Payment payment) {
        log.debug("Saving payment: {}", payment.getId());
        PaymentJpaEntity entity = mapper.toJpaEntity(payment);
        PaymentJpaEntity savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        log.debug("Finding payment by ID: {}", paymentId);
        return springDataRepository.findById(paymentId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<Payment> findByCustomerId(String customerId) {
        log.debug("Finding payments for customer: {}", customerId);
        return springDataRepository.findByCustomerId(customerId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByOrderId(String orderId) {
        log.debug("Finding payments for order: {}", orderId);
        return springDataRepository.findByOrderId(orderId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(PaymentId paymentId) {
        return springDataRepository.existsById(paymentId.getValue());
    }

    @Override
    public void deleteById(PaymentId paymentId) {
        log.debug("Deleting payment: {}", paymentId);
        springDataRepository.deleteById(paymentId.getValue());
    }
}
