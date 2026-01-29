package com.example.payment.adapter.input.rest;

import com.example.payment.adapter.input.rest.dto.PaymentResponse;
import com.example.payment.adapter.input.rest.dto.ProcessPaymentRequest;
import com.example.payment.adapter.input.rest.dto.RefundPaymentRequest;
import com.example.payment.application.port.input.GetPaymentStatusUseCase;
import com.example.payment.application.port.input.ProcessPaymentUseCase;
import com.example.payment.application.port.input.ProcessPaymentUseCase.ProcessPaymentCommand;
import com.example.payment.application.port.input.RefundPaymentUseCase;
import com.example.payment.application.port.input.RefundPaymentUseCase.RefundPaymentCommand;
import com.example.payment.domain.entity.Payment;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller adapter for payment operations.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final GetPaymentStatusUseCase getPaymentStatusUseCase;

    public PaymentController(ProcessPaymentUseCase processPaymentUseCase,
                             RefundPaymentUseCase refundPaymentUseCase,
                             GetPaymentStatusUseCase getPaymentStatusUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.refundPaymentUseCase = refundPaymentUseCase;
        this.getPaymentStatusUseCase = getPaymentStatusUseCase;
    }

    /**
     * Process a new payment.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        log.info("Received payment request for order: {}", request.orderId());

        ProcessPaymentCommand command = new ProcessPaymentCommand(
                request.orderId(),
                request.customerId(),
                request.amount(),
                request.currency(),
                request.paymentMethod(),
                request.customerEmail()
        );

        Payment payment = processPaymentUseCase.processPayment(command);
        PaymentResponse response = PaymentResponse.fromDomain(payment);

        HttpStatus status = payment.getStatus().isSuccessful() ?
                HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Get payment by ID.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        log.info("Getting payment: {}", paymentId);

        var statusResponse = getPaymentStatusUseCase.getPaymentStatus(paymentId);
        return ResponseEntity.ok(PaymentResponse.fromStatusResponse(statusResponse));
    }

    /**
     * Refund a payment.
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody RefundPaymentRequest request) {
        log.info("Received refund request for payment: {}", paymentId);

        RefundPaymentCommand command = new RefundPaymentCommand(
                paymentId,
                request.amount(),
                request.reason(),
                request.customerEmail()
        );

        Payment payment = refundPaymentUseCase.refundPayment(command);
        return ResponseEntity.ok(PaymentResponse.fromDomain(payment));
    }

    /**
     * Get payments by customer ID.
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByCustomer(@PathVariable String customerId) {
        log.info("Getting payments for customer: {}", customerId);

        List<PaymentResponse> responses = getPaymentStatusUseCase.getPaymentsByCustomer(customerId)
                .stream()
                .map(PaymentResponse::fromStatusResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get payments by order ID.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrder(@PathVariable String orderId) {
        log.info("Getting payments for order: {}", orderId);

        List<PaymentResponse> responses = getPaymentStatusUseCase.getPaymentsByOrder(orderId)
                .stream()
                .map(PaymentResponse::fromStatusResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
