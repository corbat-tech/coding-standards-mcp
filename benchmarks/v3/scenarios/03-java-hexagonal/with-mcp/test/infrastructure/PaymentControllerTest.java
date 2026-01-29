package com.payment.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.domain.exception.PaymentNotFoundException;
import com.payment.domain.port.input.GetPaymentStatusUseCase;
import com.payment.domain.port.input.ProcessPaymentUseCase;
import com.payment.domain.port.input.RefundPaymentUseCase;
import com.payment.domain.valueobject.Money;
import com.payment.domain.valueobject.PaymentId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("Payment Controller")
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

    @Nested
    @DisplayName("POST /api/v1/payments")
    class ProcessPayment {

        @Test
        @DisplayName("should return 201 when payment processed successfully")
        void shouldReturn201WhenPaymentProcessedSuccessfully() throws Exception {
            // Given
            PaymentId paymentId = PaymentId.generate();
            var result = ProcessPaymentUseCase.ProcessPaymentResult.success(paymentId, "txn_abc123");
            when(processPaymentUseCase.execute(any())).thenReturn(result);

            String requestBody = """
                {
                    "orderId": "order-123",
                    "customerId": "customer-456",
                    "amount": 100.00,
                    "currency": "USD",
                    "paymentMethod": "card"
                }
                """;

            // When/Then
            mockMvc.perform(post("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").value("txn_abc123"));
        }

        @Test
        @DisplayName("should return 200 when payment fails")
        void shouldReturn200WhenPaymentFails() throws Exception {
            // Given
            PaymentId paymentId = PaymentId.generate();
            var result = ProcessPaymentUseCase.ProcessPaymentResult.failed(paymentId, "Card declined");
            when(processPaymentUseCase.execute(any())).thenReturn(result);

            String requestBody = """
                {
                    "orderId": "order-123",
                    "customerId": "customer-456",
                    "amount": 100.00,
                    "currency": "USD",
                    "paymentMethod": "card"
                }
                """;

            // When/Then
            mockMvc.perform(post("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
        }

        @Test
        @DisplayName("should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            String requestBody = """
                {
                    "orderId": "",
                    "customerId": "customer-456",
                    "amount": -100.00,
                    "currency": "USD",
                    "paymentMethod": "card"
                }
                """;

            mockMvc.perform(post("/api/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/refund")
    class RefundPayment {

        @Test
        @DisplayName("should return 200 when refund processed")
        void shouldReturn200WhenRefundProcessed() throws Exception {
            // Given
            PaymentId paymentId = PaymentId.generate();
            var result = RefundPaymentUseCase.RefundResult.success(
                paymentId, Money.usd(new BigDecimal("100.00"))
            );
            when(refundPaymentUseCase.execute(any())).thenReturn(result);

            String requestBody = """
                {
                    "amount": 100.00,
                    "currency": "USD",
                    "reason": "Customer request"
                }
                """;

            // When/Then
            mockMvc.perform(post("/api/v1/payments/{id}/refund", paymentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("REFUNDED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{id}")
    class GetPaymentStatus {

        @Test
        @DisplayName("should return payment status when found")
        void shouldReturnPaymentStatusWhenFound() throws Exception {
            // Given
            PaymentId paymentId = PaymentId.generate();
            var response = new GetPaymentStatusUseCase.PaymentStatusResponse(
                paymentId, "order-123", "customer-456",
                Money.usd(new BigDecimal("100.00")),
                Money.usd(BigDecimal.ZERO),
                "COMPLETED", "txn_abc123",
                Instant.now(), Instant.now()
            );
            when(getPaymentStatusUseCase.execute(any())).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("should return 404 when payment not found")
        void shouldReturn404WhenPaymentNotFound() throws Exception {
            // Given
            PaymentId paymentId = PaymentId.generate();
            when(getPaymentStatusUseCase.execute(any()))
                .thenThrow(new PaymentNotFoundException(paymentId));

            // When/Then
            mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
        }
    }
}
