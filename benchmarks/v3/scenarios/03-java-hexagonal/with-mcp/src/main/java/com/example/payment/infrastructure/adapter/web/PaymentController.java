package com.example.payment.infrastructure.adapter.web;

import com.example.payment.application.port.input.*;
import com.example.payment.domain.entity.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

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

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        Payment payment = processPaymentUseCase.process(
                request.orderId(), request.amount(), request.currency(), request.cardToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String id) {
        Payment payment = refundPaymentUseCase.refund(id);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable String id) {
        Payment payment = getPaymentStatusUseCase.getStatus(id);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    public record ProcessPaymentRequest(
            @NotBlank String orderId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency,
            @NotBlank String cardToken
    ) {}

    public record PaymentResponse(
            String id,
            String orderId,
            BigDecimal amount,
            String currency,
            String status
    ) {
        public static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                    payment.getId().toString(),
                    payment.getOrderId(),
                    payment.getAmount().getAmount(),
                    payment.getAmount().getCurrency().getCurrencyCode(),
                    payment.getStatus().name()
            );
        }
    }
}
