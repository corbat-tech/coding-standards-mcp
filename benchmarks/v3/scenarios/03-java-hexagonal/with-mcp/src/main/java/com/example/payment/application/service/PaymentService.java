package com.example.payment.application.service;

import com.example.payment.application.port.input.*;
import com.example.payment.application.port.output.*;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.exception.*;
import com.example.payment.domain.valueobject.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class PaymentService implements ProcessPaymentUseCase, RefundPaymentUseCase, GetPaymentStatusUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentGateway paymentGateway,
                          NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.notificationService = notificationService;
    }

    @Override
    public Payment process(String orderId, BigDecimal amount, String currency, String cardToken) {
        Money money = Money.of(amount, currency);
        Payment payment = new Payment(PaymentId.generate(), orderId, money);
        payment.markProcessing();
        paymentRepository.save(payment);

        PaymentGateway.GatewayResponse response = paymentGateway.charge(cardToken, money);

        if (response.success()) {
            payment.complete(response.transactionId());
            notificationService.notifyPaymentCompleted(payment);
        } else {
            payment.fail();
            notificationService.notifyPaymentFailed(payment);
        }
        return paymentRepository.save(payment);
    }

    @Override
    public Payment refund(String paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        payment.refund();

        PaymentGateway.GatewayResponse response = paymentGateway.refund(
                payment.getGatewayTransactionId(), payment.getAmount());

        if (!response.success()) {
            throw new PaymentProcessingException("Refund failed: " + response.errorMessage());
        }
        notificationService.notifyPaymentRefunded(payment);
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getStatus(String paymentId) {
        return findPaymentOrThrow(paymentId);
    }

    private Payment findPaymentOrThrow(String paymentId) {
        return paymentRepository.findById(PaymentId.from(paymentId))
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
