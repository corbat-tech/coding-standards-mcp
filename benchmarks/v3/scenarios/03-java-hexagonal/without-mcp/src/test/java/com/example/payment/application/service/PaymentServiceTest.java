package com.example.payment.application.service;

import com.example.payment.application.port.input.GetPaymentStatusUseCase.PaymentStatusResponse;
import com.example.payment.application.port.input.ProcessPaymentUseCase.ProcessPaymentCommand;
import com.example.payment.application.port.input.RefundPaymentUseCase.RefundPaymentCommand;
import com.example.payment.application.port.output.NotificationService;
import com.example.payment.application.port.output.PaymentGateway;
import com.example.payment.application.port.output.PaymentGateway.PaymentGatewayResponse;
import com.example.payment.application.port.output.PaymentGateway.RefundGatewayResponse;
import com.example.payment.application.port.output.PaymentRepository;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.exception.InvalidPaymentOperationException;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.valueobject.Money;
import com.example.payment.domain.valueobject.PaymentId;
import com.example.payment.domain.valueobject.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentGateway, notificationService);
    }

    @Nested
    @DisplayName("Process Payment")
    class ProcessPayment {

        private ProcessPaymentCommand validCommand;

        @BeforeEach
        void setUp() {
            validCommand = new ProcessPaymentCommand(
                    "order-123",
                    "customer-456",
                    new BigDecimal("100.00"),
                    "USD",
                    "card_token",
                    "customer@example.com"
            );

            when(paymentRepository.save(any(Payment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("should process payment successfully")
        void shouldProcessPaymentSuccessfully() {
            when(paymentGateway.processPayment(any()))
                    .thenReturn(PaymentGatewayResponse.success("txn-123"));

            Payment result = paymentService.processPayment(validCommand);

            assertEquals(PaymentStatus.COMPLETED, result.getStatus());
            assertEquals("txn-123", result.getGatewayTransactionId());
            verify(paymentRepository, times(3)).save(any(Payment.class));
            verify(notificationService).sendPaymentConfirmation(any(), eq("customer@example.com"));
        }

        @Test
        @DisplayName("should handle payment failure from gateway")
        void shouldHandlePaymentFailureFromGateway() {
            when(paymentGateway.processPayment(any()))
                    .thenReturn(PaymentGatewayResponse.failure("card_declined", "Card was declined"));

            Payment result = paymentService.processPayment(validCommand);

            assertEquals(PaymentStatus.FAILED, result.getStatus());
            assertTrue(result.getFailureReason().contains("card_declined"));
            verify(notificationService).sendPaymentFailure(any(), eq("customer@example.com"));
        }

        @Test
        @DisplayName("should save payment in correct states")
        void shouldSavePaymentInCorrectStates() {
            when(paymentGateway.processPayment(any()))
                    .thenReturn(PaymentGatewayResponse.success("txn-123"));

            paymentService.processPayment(validCommand);

            verify(paymentRepository, times(3)).save(paymentCaptor.capture());
            List<Payment> savedPayments = paymentCaptor.getAllValues();

            assertEquals(PaymentStatus.PENDING, savedPayments.get(0).getStatus());
            assertEquals(PaymentStatus.PROCESSING, savedPayments.get(1).getStatus());
            assertEquals(PaymentStatus.COMPLETED, savedPayments.get(2).getStatus());
        }

        @Test
        @DisplayName("should not send notification when email is null")
        void shouldNotSendNotificationWhenEmailIsNull() {
            ProcessPaymentCommand commandWithoutEmail = new ProcessPaymentCommand(
                    "order-123", "customer-456",
                    new BigDecimal("100.00"), "USD", "card_token", null);

            when(paymentGateway.processPayment(any()))
                    .thenReturn(PaymentGatewayResponse.success("txn-123"));

            paymentService.processPayment(commandWithoutEmail);

            verify(notificationService, never()).sendPaymentConfirmation(any(), any());
        }
    }

    @Nested
    @DisplayName("Refund Payment")
    class RefundPayment {

        private Payment completedPayment;
        private RefundPaymentCommand refundCommand;

        @BeforeEach
        void setUp() {
            completedPayment = Payment.builder()
                    .id(PaymentId.of("550e8400-e29b-41d4-a716-446655440000"))
                    .orderId("order-123")
                    .customerId("customer-456")
                    .amount(Money.of(new BigDecimal("100.00"), "USD"))
                    .status(PaymentStatus.COMPLETED)
                    .refundedAmount(Money.zero("USD"))
                    .gatewayTransactionId("ch_original123")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            refundCommand = new RefundPaymentCommand(
                    "550e8400-e29b-41d4-a716-446655440000",
                    new BigDecimal("50.00"),
                    "Customer request",
                    "customer@example.com"
            );

            when(paymentRepository.findById(any()))
                    .thenReturn(Optional.of(completedPayment));
            when(paymentRepository.save(any(Payment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("should process refund successfully")
        void shouldProcessRefundSuccessfully() {
            when(paymentGateway.processRefund(any()))
                    .thenReturn(RefundGatewayResponse.success("re_refund123"));

            Payment result = paymentService.refundPayment(refundCommand);

            assertEquals(PaymentStatus.PARTIALLY_REFUNDED, result.getStatus());
            assertEquals(Money.of(new BigDecimal("50.00"), "USD"), result.getRefundedAmount());
            verify(notificationService).sendRefundConfirmation(any(), eq("customer@example.com"));
        }

        @Test
        @DisplayName("should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            when(paymentRepository.findById(any()))
                    .thenReturn(Optional.empty());

            assertThrows(PaymentNotFoundException.class, () ->
                    paymentService.refundPayment(refundCommand));
        }

        @Test
        @DisplayName("should throw exception when refund amount exceeds refundable")
        void shouldThrowExceptionWhenRefundAmountExceedsRefundable() {
            RefundPaymentCommand excessRefund = new RefundPaymentCommand(
                    "550e8400-e29b-41d4-a716-446655440000",
                    new BigDecimal("150.00"),
                    "Customer request",
                    null
            );

            assertThrows(InvalidPaymentOperationException.class, () ->
                    paymentService.refundPayment(excessRefund));
        }

        @Test
        @DisplayName("should throw exception when gateway fails")
        void shouldThrowExceptionWhenGatewayFails() {
            when(paymentGateway.processRefund(any()))
                    .thenReturn(RefundGatewayResponse.failure("refund_failed", "Refund could not be processed"));

            assertThrows(InvalidPaymentOperationException.class, () ->
                    paymentService.refundPayment(refundCommand));
        }
    }

    @Nested
    @DisplayName("Get Payment Status")
    class GetPaymentStatus {

        private Payment payment;

        @BeforeEach
        void setUp() {
            payment = Payment.builder()
                    .id(PaymentId.of("550e8400-e29b-41d4-a716-446655440000"))
                    .orderId("order-123")
                    .customerId("customer-456")
                    .amount(Money.of(new BigDecimal("100.00"), "USD"))
                    .status(PaymentStatus.COMPLETED)
                    .refundedAmount(Money.zero("USD"))
                    .gatewayTransactionId("ch_123")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("should return payment status by ID")
        void shouldReturnPaymentStatusById() {
            when(paymentRepository.findById(any()))
                    .thenReturn(Optional.of(payment));

            PaymentStatusResponse response = paymentService.getPaymentStatus(
                    "550e8400-e29b-41d4-a716-446655440000");

            assertEquals("550e8400-e29b-41d4-a716-446655440000", response.paymentId());
            assertEquals(PaymentStatus.COMPLETED, response.status());
            assertEquals("order-123", response.orderId());
        }

        @Test
        @DisplayName("should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            when(paymentRepository.findById(any()))
                    .thenReturn(Optional.empty());

            assertThrows(PaymentNotFoundException.class, () ->
                    paymentService.getPaymentStatus("550e8400-e29b-41d4-a716-446655440000"));
        }

        @Test
        @DisplayName("should return payments by customer ID")
        void shouldReturnPaymentsByCustomerId() {
            when(paymentRepository.findByCustomerId("customer-456"))
                    .thenReturn(List.of(payment));

            List<PaymentStatusResponse> responses = paymentService.getPaymentsByCustomer("customer-456");

            assertEquals(1, responses.size());
            assertEquals("customer-456", responses.get(0).customerId());
        }

        @Test
        @DisplayName("should return payments by order ID")
        void shouldReturnPaymentsByOrderId() {
            when(paymentRepository.findByOrderId("order-123"))
                    .thenReturn(List.of(payment));

            List<PaymentStatusResponse> responses = paymentService.getPaymentsByOrder("order-123");

            assertEquals(1, responses.size());
            assertEquals("order-123", responses.get(0).orderId());
        }
    }
}
