package com.payment.infrastructure.adapter.in.rest;

import com.payment.domain.port.input.GetPaymentStatusUseCase;
import com.payment.domain.port.input.ProcessPaymentUseCase;
import com.payment.domain.port.input.RefundPaymentUseCase;
import com.payment.domain.valueobject.Money;
import com.payment.domain.valueobject.PaymentId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * REST adapter for payment operations.
 * Primary/driving adapter that exposes HTTP endpoints.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final GetPaymentStatusUseCase getPaymentStatusUseCase;

    public PaymentController(
            ProcessPaymentUseCase processPaymentUseCase,
            RefundPaymentUseCase refundPaymentUseCase,
            GetPaymentStatusUseCase getPaymentStatusUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.refundPaymentUseCase = refundPaymentUseCase;
        this.getPaymentStatusUseCase = getPaymentStatusUseCase;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        var command = new ProcessPaymentUseCase.ProcessPaymentCommand(
            request.orderId(),
            request.customerId(),
            Money.of(request.amount(), request.currency()),
            request.paymentMethod()
        );

        var result = processPaymentUseCase.execute(command);

        return ResponseEntity
            .status(result.status().equals("COMPLETED") ? HttpStatus.CREATED : HttpStatus.OK)
            .body(PaymentResponse.from(result));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<RefundResponse> refundPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody RefundRequest request) {
        var command = new RefundPaymentUseCase.RefundCommand(
            PaymentId.of(paymentId),
            Money.of(request.amount(), request.currency()),
            request.reason()
        );

        var result = refundPaymentUseCase.execute(command);

        return ResponseEntity.ok(RefundResponse.from(result));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable String paymentId) {
        var result = getPaymentStatusUseCase.execute(PaymentId.of(paymentId));
        return ResponseEntity.ok(PaymentStatusResponse.from(result));
    }

    // Request DTOs
    record ProcessPaymentRequest(
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String paymentMethod
    ) {}

    record RefundRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        String reason
    ) {}

    // Response DTOs
    record PaymentResponse(String paymentId, String status, String transactionId, String message) {
        static PaymentResponse from(ProcessPaymentUseCase.ProcessPaymentResult result) {
            return new PaymentResponse(
                result.paymentId().toString(),
                result.status(),
                result.transactionId(),
                result.message()
            );
        }
    }

    record RefundResponse(String paymentId, boolean success, String status, String amount, String message) {
        static RefundResponse from(RefundPaymentUseCase.RefundResult result) {
            return new RefundResponse(
                result.paymentId().toString(),
                result.success(),
                result.status(),
                result.refundedAmount() != null ? result.refundedAmount().toString() : null,
                result.message()
            );
        }
    }

    record PaymentStatusResponse(
        String paymentId, String orderId, String customerId, String amount,
        String refundedAmount, String status, String transactionId
    ) {
        static PaymentStatusResponse from(GetPaymentStatusUseCase.PaymentStatusResponse result) {
            return new PaymentStatusResponse(
                result.paymentId().toString(),
                result.orderId(),
                result.customerId(),
                result.amount().toString(),
                result.refundedAmount().toString(),
                result.status(),
                result.gatewayTransactionId()
            );
        }
    }
}
