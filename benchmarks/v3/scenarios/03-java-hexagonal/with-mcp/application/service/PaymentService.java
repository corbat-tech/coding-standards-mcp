package com.payment.application.service;

import com.payment.domain.entity.Payment;
import com.payment.domain.entity.PaymentStatus;
import com.payment.domain.exception.PaymentGatewayException;
import com.payment.domain.exception.PaymentNotFoundException;
import com.payment.domain.port.input.GetPaymentStatusUseCase;
import com.payment.domain.port.input.ProcessPaymentUseCase;
import com.payment.domain.port.input.RefundPaymentUseCase;
import com.payment.domain.port.output.NotificationService;
import com.payment.domain.port.output.PaymentGateway;
import com.payment.domain.port.output.PaymentRepository;
import com.payment.domain.valueobject.PaymentId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementing all payment use cases.
 * Orchestrates domain logic and infrastructure interactions.
 */
@Service
@Transactional
public class PaymentService implements ProcessPaymentUseCase, RefundPaymentUseCase, GetPaymentStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.notificationService = notificationService;
    }

    @Override
    public ProcessPaymentResult execute(ProcessPaymentCommand command) {
        log.info("Processing payment for order: {}", command.orderId());

        Payment payment = Payment.create(command.orderId(), command.customerId(), command.amount());
        payment = paymentRepository.save(payment);

        payment.markAsProcessing();
        payment = paymentRepository.save(payment);

        PaymentGateway.ChargeResult chargeResult = processGatewayCharge(command, payment);

        if (chargeResult.success()) {
            return handleSuccessfulPayment(payment, chargeResult.transactionId());
        } else {
            return handleFailedPayment(payment, chargeResult.errorMessage());
        }
    }

    private PaymentGateway.ChargeResult processGatewayCharge(ProcessPaymentCommand command, Payment payment) {
        PaymentGateway.ChargeRequest chargeRequest = new PaymentGateway.ChargeRequest(
            command.customerId(),
            command.orderId(),
            command.amount(),
            command.paymentMethod(),
            payment.getId().toString()
        );

        try {
            return paymentGateway.charge(chargeRequest);
        } catch (Exception e) {
            log.error("Gateway error for payment {}: {}", payment.getId(), e.getMessage());
            throw new PaymentGatewayException("Payment gateway error", e);
        }
    }

    private ProcessPaymentResult handleSuccessfulPayment(Payment payment, String transactionId) {
        payment.markAsCompleted(transactionId);
        payment = paymentRepository.save(payment);
        notificationService.notifyPaymentSuccess(payment);
        log.info("Payment {} completed successfully", payment.getId());
        return ProcessPaymentResult.success(payment.getId(), transactionId);
    }

    private ProcessPaymentResult handleFailedPayment(Payment payment, String reason) {
        payment.markAsFailed();
        paymentRepository.save(payment);
        notificationService.notifyPaymentFailure(payment, reason);
        log.warn("Payment {} failed: {}", payment.getId(), reason);
        return ProcessPaymentResult.failed(payment.getId(), reason);
    }

    @Override
    public RefundResult execute(RefundCommand command) {
        log.info("Processing refund for payment: {}", command.paymentId());

        Payment payment = paymentRepository.findById(command.paymentId())
            .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        PaymentGateway.RefundRequest refundRequest = new PaymentGateway.RefundRequest(
            payment.getGatewayTransactionId(),
            command.amount(),
            command.reason()
        );

        PaymentGateway.RefundResult gatewayResult = paymentGateway.refund(refundRequest);

        if (!gatewayResult.success()) {
            log.warn("Refund failed for payment {}: {}", command.paymentId(), gatewayResult.errorMessage());
            return RefundResult.failed(command.paymentId(), gatewayResult.errorMessage());
        }

        payment.refund(command.amount());
        payment = paymentRepository.save(payment);
        notificationService.notifyRefundProcessed(payment);

        log.info("Refund processed for payment {}, status: {}", payment.getId(), payment.getStatus());

        return payment.getStatus() == PaymentStatus.REFUNDED
            ? RefundResult.success(payment.getId(), command.amount())
            : RefundResult.partialSuccess(payment.getId(), command.amount());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse execute(PaymentId paymentId) {
        log.debug("Getting status for payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        return new PaymentStatusResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getCustomerId(),
            payment.getAmount(),
            payment.getRefundedAmount(),
            payment.getStatus().name(),
            payment.getGatewayTransactionId(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
