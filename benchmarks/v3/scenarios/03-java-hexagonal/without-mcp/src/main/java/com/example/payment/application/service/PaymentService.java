package com.example.payment.application.service;

import com.example.payment.application.port.input.GetPaymentStatusUseCase;
import com.example.payment.application.port.input.ProcessPaymentUseCase;
import com.example.payment.application.port.input.RefundPaymentUseCase;
import com.example.payment.application.port.output.NotificationService;
import com.example.payment.application.port.output.PaymentGateway;
import com.example.payment.application.port.output.PaymentGateway.PaymentGatewayRequest;
import com.example.payment.application.port.output.PaymentGateway.RefundGatewayRequest;
import com.example.payment.application.port.output.PaymentRepository;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.exception.InvalidPaymentOperationException;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.valueobject.Money;
import com.example.payment.domain.valueobject.PaymentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service implementing all payment use cases.
 * Orchestrates domain entities and output ports.
 */
@Service
@Transactional
public class PaymentService implements ProcessPaymentUseCase, RefundPaymentUseCase, GetPaymentStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

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
    public Payment processPayment(ProcessPaymentCommand command) {
        log.info("Processing payment for order: {}", command.orderId());

        // Create domain entity
        Money amount = Money.of(command.amount(), command.currency());
        Payment payment = Payment.create(command.orderId(), command.customerId(), amount);

        // Save initial state
        payment = paymentRepository.save(payment);
        log.debug("Created payment with ID: {}", payment.getId());

        // Start processing
        payment.startProcessing();
        payment = paymentRepository.save(payment);

        // Call payment gateway
        var gatewayRequest = new PaymentGatewayRequest(
                payment.getId().toString(),
                payment.getAmount(),
                command.paymentMethod(),
                command.customerId(),
                command.orderId()
        );

        var gatewayResponse = paymentGateway.processPayment(gatewayRequest);

        if (gatewayResponse.success()) {
            payment.complete(gatewayResponse.transactionId());
            payment = paymentRepository.save(payment);
            log.info("Payment {} completed successfully with transaction ID: {}",
                    payment.getId(), gatewayResponse.transactionId());

            // Send confirmation notification
            if (command.customerEmail() != null) {
                notificationService.sendPaymentConfirmation(payment, command.customerEmail());
            }
        } else {
            String failureReason = gatewayResponse.errorCode() + ": " + gatewayResponse.errorMessage();
            payment.fail(failureReason);
            payment = paymentRepository.save(payment);
            log.warn("Payment {} failed: {}", payment.getId(), failureReason);

            // Send failure notification
            if (command.customerEmail() != null) {
                notificationService.sendPaymentFailure(payment, command.customerEmail());
            }
        }

        return payment;
    }

    @Override
    public Payment refundPayment(RefundPaymentCommand command) {
        log.info("Processing refund for payment: {}", command.paymentId());

        // Find the payment
        PaymentId paymentId = PaymentId.of(command.paymentId());
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Create refund amount
        Money refundAmount = Money.of(command.amount(), payment.getAmount().getCurrencyCode());

        // Validate refund can be processed
        if (!payment.canRefund(refundAmount)) {
            throw new InvalidPaymentOperationException(
                    "Cannot refund payment. Status: " + payment.getStatus() +
                            ", Refundable amount: " + payment.getRefundableAmount());
        }

        // Initiate refund in domain
        payment.initiateRefund(refundAmount);
        payment = paymentRepository.save(payment);

        // Process refund through gateway
        var refundRequest = new RefundGatewayRequest(
                payment.getGatewayTransactionId(),
                refundAmount,
                command.reason()
        );

        var refundResponse = paymentGateway.processRefund(refundRequest);

        if (refundResponse.success()) {
            payment.completeRefund(refundAmount);
            payment = paymentRepository.save(payment);
            log.info("Refund completed for payment {}. Refund transaction ID: {}",
                    payment.getId(), refundResponse.refundTransactionId());

            // Send confirmation notification
            if (command.customerEmail() != null) {
                notificationService.sendRefundConfirmation(payment, command.customerEmail());
            }
        } else {
            String failureReason = refundResponse.errorCode() + ": " + refundResponse.errorMessage();
            log.warn("Refund failed for payment {}: {}", payment.getId(), failureReason);

            // Revert to previous state (COMPLETED or PARTIALLY_REFUNDED)
            // Note: In a real system, you might want to track refund attempts separately
            throw new InvalidPaymentOperationException("Refund failed: " + failureReason);
        }

        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String paymentId) {
        log.debug("Getting status for payment: {}", paymentId);

        PaymentId id = PaymentId.of(paymentId);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        return PaymentStatusResponse.fromPayment(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentStatusResponse> getPaymentsByCustomer(String customerId) {
        log.debug("Getting payments for customer: {}", customerId);

        return paymentRepository.findByCustomerId(customerId)
                .stream()
                .map(PaymentStatusResponse::fromPayment)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentStatusResponse> getPaymentsByOrder(String orderId) {
        log.debug("Getting payments for order: {}", orderId);

        return paymentRepository.findByOrderId(orderId)
                .stream()
                .map(PaymentStatusResponse::fromPayment)
                .collect(Collectors.toList());
    }
}
