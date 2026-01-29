package com.payment.application.service;

import com.payment.domain.entity.Payment;
import com.payment.domain.entity.PaymentStatus;
import com.payment.domain.exception.PaymentNotFoundException;
import com.payment.domain.port.input.GetPaymentStatusUseCase;
import com.payment.domain.port.input.ProcessPaymentUseCase;
import com.payment.domain.port.input.RefundPaymentUseCase;
import com.payment.domain.port.output.NotificationService;
import com.payment.domain.port.output.PaymentGateway;
import com.payment.domain.port.output.PaymentRepository;
import com.payment.domain.valueobject.Money;
import com.payment.domain.valueobject.PaymentId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private NotificationService notificationService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentGateway, notificationService);
    }

    @Nested
    @DisplayName("Process Payment")
    class ProcessPayment {

        @Test
        @DisplayName("should process payment successfully when gateway approves")
        void shouldProcessPaymentSuccessfullyWhenGatewayApproves() {
            // Given
            var command = new ProcessPaymentUseCase.ProcessPaymentCommand(
                "order-123", "customer-456",
                Money.usd(new BigDecimal("100.00")), "card"
            );

            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(paymentGateway.charge(any()))
                .thenReturn(PaymentGateway.ChargeResult.success("txn_abc123"));

            // When
            var result = paymentService.execute(command);

            // Then
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.transactionId()).isEqualTo("txn_abc123");
            verify(notificationService).notifyPaymentSuccess(any());
        }

        @Test
        @DisplayName("should mark payment as failed when gateway declines")
        void shouldMarkPaymentAsFailedWhenGatewayDeclines() {
            // Given
            var command = new ProcessPaymentUseCase.ProcessPaymentCommand(
                "order-123", "customer-456",
                Money.usd(new BigDecimal("100.00")), "card"
            );

            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(paymentGateway.charge(any()))
                .thenReturn(PaymentGateway.ChargeResult.failure("declined", "Card declined"));

            // When
            var result = paymentService.execute(command);

            // Then
            assertThat(result.status()).isEqualTo("FAILED");
            assertThat(result.message()).isEqualTo("Card declined");
            verify(notificationService).notifyPaymentFailure(any(), eq("Card declined"));
        }

        @Test
        @DisplayName("should save payment three times during processing")
        void shouldSavePaymentThreeTimesDuringProcessing() {
            // Given
            var command = new ProcessPaymentUseCase.ProcessPaymentCommand(
                "order-123", "customer-456",
                Money.usd(new BigDecimal("100.00")), "card"
            );

            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(paymentGateway.charge(any()))
                .thenReturn(PaymentGateway.ChargeResult.success("txn_abc123"));

            // When
            paymentService.execute(command);

            // Then: PENDING -> PROCESSING -> COMPLETED = 3 saves
            verify(paymentRepository, times(3)).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("Refund Payment")
    class RefundPayment {

        @Test
        @DisplayName("should refund payment successfully")
        void shouldRefundPaymentSuccessfully() {
            // Given
            PaymentId paymentId = PaymentId.generate();
            Payment payment = createCompletedPayment(paymentId);
            var command = new RefundPaymentUseCase.RefundCommand(
                paymentId,
                Money.usd(new BigDecimal("100.00")),
                "Customer request"
            );

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
            when(paymentGateway.refund(any()))
                .thenReturn(PaymentGateway.RefundResult.success("re_abc123"));
            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            var result = paymentService.execute(command);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.status()).isEqualTo("REFUNDED");
            verify(notificationService).notifyRefundProcessed(any());
        }

        @Test
        @DisplayName("should return partial refund status when amount is less than total")
        void shouldReturnPartialRefundStatusWhenAmountIsLessThanTotal() {
            // Given
            PaymentId paymentId = PaymentId.generate();
            Payment payment = createCompletedPayment(paymentId);
            var command = new RefundPaymentUseCase.RefundCommand(
                paymentId,
                Money.usd(new BigDecimal("50.00")),
                "Partial refund"
            );

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
            when(paymentGateway.refund(any()))
                .thenReturn(PaymentGateway.RefundResult.success("re_abc123"));
            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            var result = paymentService.execute(command);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.status()).isEqualTo("PARTIALLY_REFUNDED");
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            // Given
            PaymentId paymentId = PaymentId.generate();
            var command = new RefundPaymentUseCase.RefundCommand(
                paymentId,
                Money.usd(new BigDecimal("100.00")),
                "Customer request"
            );

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> paymentService.execute(command))
                .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("should return failure when gateway rejects refund")
        void shouldReturnFailureWhenGatewayRejectsRefund() {
            // Given
            PaymentId paymentId = PaymentId.generate();
            Payment payment = createCompletedPayment(paymentId);
            var command = new RefundPaymentUseCase.RefundCommand(
                paymentId,
                Money.usd(new BigDecimal("100.00")),
                "Customer request"
            );

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
            when(paymentGateway.refund(any()))
                .thenReturn(PaymentGateway.RefundResult.failure("error", "Refund not allowed"));

            // When
            var result = paymentService.execute(command);

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Refund not allowed");
        }
    }

    @Nested
    @DisplayName("Get Payment Status")
    class GetPaymentStatus {

        @Test
        @DisplayName("should return payment status when found")
        void shouldReturnPaymentStatusWhenFound() {
            // Given
            PaymentId paymentId = PaymentId.generate();
            Payment payment = createCompletedPayment(paymentId);

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            // When
            GetPaymentStatusUseCase.PaymentStatusResponse result = paymentService.execute(paymentId);

            // Then
            assertThat(result.paymentId()).isEqualTo(paymentId);
            assertThat(result.status()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            // Given
            PaymentId paymentId = PaymentId.generate();
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> paymentService.execute(paymentId))
                .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    private Payment createCompletedPayment(PaymentId paymentId) {
        Payment payment = Payment.create("order-123", "customer-456",
            Money.usd(new BigDecimal("100.00")));
        payment.markAsProcessing();
        payment.markAsCompleted("txn_original");
        return Payment.reconstitute(
            paymentId,
            payment.getOrderId(),
            payment.getCustomerId(),
            payment.getAmount(),
            payment.getRefundedAmount(),
            payment.getStatus(),
            payment.getGatewayTransactionId(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
