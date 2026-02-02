package com.example.payment.application.service;

import com.example.payment.application.port.output.*;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.exception.*;
import com.example.payment.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private NotificationService notificationService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentGateway, notificationService);
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(), any()))
                .thenReturn(PaymentGateway.GatewayResponse.success("txn_123"));

        Payment result = paymentService.process("order-1", new BigDecimal("100.00"), "USD", "card_token");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getGatewayTransactionId()).isEqualTo("txn_123");
        verify(notificationService).notifyPaymentCompleted(any());
    }

    @Test
    void shouldMarkPaymentAsFailedWhenGatewayFails() {
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(), any()))
                .thenReturn(PaymentGateway.GatewayResponse.failure("Card declined"));

        Payment result = paymentService.process("order-1", new BigDecimal("100.00"), "USD", "card_token");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(notificationService).notifyPaymentFailed(any());
    }

    @Test
    void shouldRefundCompletedPayment() {
        Payment payment = new Payment(PaymentId.generate(), "order-1", Money.usd(100.00));
        payment.markProcessing();
        payment.complete("txn_123");
        when(paymentRepository.findById(any())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.refund(any(), any()))
                .thenReturn(PaymentGateway.GatewayResponse.success("ref_123"));

        Payment result = paymentService.refund(payment.getId().toString());

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(notificationService).notifyPaymentRefunded(any());
    }

    @Test
    void shouldThrowWhenRefundingNonCompletedPayment() {
        Payment payment = new Payment(PaymentId.generate(), "order-1", Money.usd(100.00));
        when(paymentRepository.findById(any())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refund(payment.getId().toString()))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void shouldThrowWhenPaymentNotFound() {
        when(paymentRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getStatus("non-existent-id"))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void shouldGetPaymentStatus() {
        Payment payment = new Payment(PaymentId.generate(), "order-1", Money.usd(100.00));
        when(paymentRepository.findById(any())).thenReturn(Optional.of(payment));

        Payment result = paymentService.getStatus(payment.getId().toString());

        assertThat(result.getOrderId()).isEqualTo("order-1");
    }
}
