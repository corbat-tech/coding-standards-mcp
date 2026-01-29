package com.example.payment.adapter.input.rest;

import com.example.payment.application.port.input.GetPaymentStatusUseCase;
import com.example.payment.application.port.input.GetPaymentStatusUseCase.PaymentStatusResponse;
import com.example.payment.application.port.input.ProcessPaymentUseCase;
import com.example.payment.application.port.input.RefundPaymentUseCase;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.exception.InvalidPaymentOperationException;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.valueobject.Money;
import com.example.payment.domain.valueobject.PaymentId;
import com.example.payment.domain.valueobject.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PaymentController.
 */
@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessPaymentUseCase processPaymentUseCase;

    @MockBean
    private RefundPaymentUseCase refundPaymentUseCase;

    @MockBean
    private GetPaymentStatusUseCase getPaymentStatusUseCase;

    private Payment completedPayment;
    private PaymentStatusResponse statusResponse;

    @BeforeEach
    void setUp() {
        completedPayment = Payment.builder()
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

        statusResponse = new PaymentStatusResponse(
                "550e8400-e29b-41d4-a716-446655440000",
                "order-123",
                "customer-456",
                new BigDecimal("100.00"),
                "USD",
                PaymentStatus.COMPLETED,
                "Payment completed successfully",
                BigDecimal.ZERO,
                "ch_123",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/payments")
    class ProcessPayment {

        @Test
        @DisplayName("should process payment successfully")
        void shouldProcessPaymentSuccessfully() throws Exception {
            when(processPaymentUseCase.processPayment(any())).thenReturn(completedPayment);

            String requestBody = """
                {
                    "orderId": "order-123",
                    "customerId": "customer-456",
                    "amount": 100.00,
                    "currency": "USD",
                    "paymentMethod": "card_token",
                    "customerEmail": "customer@example.com"
                }
                """;

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").value("550e8400-e29b-41d4-a716-446655440000"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.orderId").value("order-123"));
        }

        @Test
        @DisplayName("should return 400 for missing required fields")
        void shouldReturn400ForMissingRequiredFields() throws Exception {
            String requestBody = """
                {
                    "orderId": "",
                    "customerId": "customer-456",
                    "amount": 100.00,
                    "currency": "USD",
                    "paymentMethod": "card_token"
                }
                """;

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 for invalid amount")
        void shouldReturn400ForInvalidAmount() throws Exception {
            String requestBody = """
                {
                    "orderId": "order-123",
                    "customerId": "customer-456",
                    "amount": -10.00,
                    "currency": "USD",
                    "paymentMethod": "card_token"
                }
                """;

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{paymentId}")
    class GetPayment {

        @Test
        @DisplayName("should return payment by ID")
        void shouldReturnPaymentById() throws Exception {
            when(getPaymentStatusUseCase.getPaymentStatus("550e8400-e29b-41d4-a716-446655440000"))
                    .thenReturn(statusResponse);

            mockMvc.perform(get("/api/v1/payments/550e8400-e29b-41d4-a716-446655440000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value("550e8400-e29b-41d4-a716-446655440000"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("should return 404 when payment not found")
        void shouldReturn404WhenPaymentNotFound() throws Exception {
            when(getPaymentStatusUseCase.getPaymentStatus(any()))
                    .thenThrow(new PaymentNotFoundException("550e8400-e29b-41d4-a716-446655440000"));

            mockMvc.perform(get("/api/v1/payments/550e8400-e29b-41d4-a716-446655440000"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("PAYMENT_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{paymentId}/refund")
    class RefundPayment {

        @Test
        @DisplayName("should process refund successfully")
        void shouldProcessRefundSuccessfully() throws Exception {
            Payment refundedPayment = Payment.builder()
                    .id(PaymentId.of("550e8400-e29b-41d4-a716-446655440000"))
                    .orderId("order-123")
                    .customerId("customer-456")
                    .amount(Money.of(new BigDecimal("100.00"), "USD"))
                    .status(PaymentStatus.PARTIALLY_REFUNDED)
                    .refundedAmount(Money.of(new BigDecimal("50.00"), "USD"))
                    .gatewayTransactionId("ch_123")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(refundPaymentUseCase.refundPayment(any())).thenReturn(refundedPayment);

            String requestBody = """
                {
                    "amount": 50.00,
                    "reason": "Customer request"
                }
                """;

            mockMvc.perform(post("/api/v1/payments/550e8400-e29b-41d4-a716-446655440000/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PARTIALLY_REFUNDED"))
                    .andExpect(jsonPath("$.refundedAmount").value(50.00));
        }

        @Test
        @DisplayName("should return 400 for invalid operation")
        void shouldReturn400ForInvalidOperation() throws Exception {
            when(refundPaymentUseCase.refundPayment(any()))
                    .thenThrow(new InvalidPaymentOperationException("Cannot refund pending payment"));

            String requestBody = """
                {
                    "amount": 50.00,
                    "reason": "Customer request"
                }
                """;

            mockMvc.perform(post("/api/v1/payments/550e8400-e29b-41d4-a716-446655440000/refund")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_OPERATION"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/customer/{customerId}")
    class GetPaymentsByCustomer {

        @Test
        @DisplayName("should return payments for customer")
        void shouldReturnPaymentsForCustomer() throws Exception {
            when(getPaymentStatusUseCase.getPaymentsByCustomer("customer-456"))
                    .thenReturn(List.of(statusResponse));

            mockMvc.perform(get("/api/v1/payments/customer/customer-456"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].customerId").value("customer-456"));
        }

        @Test
        @DisplayName("should return empty list when no payments found")
        void shouldReturnEmptyListWhenNoPaymentsFound() throws Exception {
            when(getPaymentStatusUseCase.getPaymentsByCustomer("customer-999"))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/payments/customer/customer-999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/order/{orderId}")
    class GetPaymentsByOrder {

        @Test
        @DisplayName("should return payments for order")
        void shouldReturnPaymentsForOrder() throws Exception {
            when(getPaymentStatusUseCase.getPaymentsByOrder("order-123"))
                    .thenReturn(List.of(statusResponse));

            mockMvc.perform(get("/api/v1/payments/order/order-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].orderId").value("order-123"));
        }
    }
}
