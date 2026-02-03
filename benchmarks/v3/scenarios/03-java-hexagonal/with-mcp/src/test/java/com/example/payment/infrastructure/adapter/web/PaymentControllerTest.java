package com.example.payment.infrastructure.adapter.web;

import com.example.payment.application.port.input.*;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.valueobject.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ProcessPaymentUseCase processPaymentUseCase;
    @MockBean private RefundPaymentUseCase refundPaymentUseCase;
    @MockBean private GetPaymentStatusUseCase getPaymentStatusUseCase;

    @Test
    void shouldProcessPayment() throws Exception {
        Payment payment = new Payment(PaymentId.generate(), "order-1", Money.usd(100.00));
        when(processPaymentUseCase.process(any(), any(), any(), any())).thenReturn(payment);

        var request = new PaymentController.ProcessPaymentRequest(
                "order-1", new BigDecimal("100.00"), "USD", "card_token");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-1"));
    }

    @Test
    void shouldGetPaymentStatus() throws Exception {
        Payment payment = new Payment(PaymentId.generate(), "order-1", Money.usd(100.00));
        when(getPaymentStatusUseCase.getStatus(any())).thenReturn(payment);

        mockMvc.perform(get("/api/payments/" + payment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentPayment() throws Exception {
        when(getPaymentStatusUseCase.getStatus(any()))
                .thenThrow(new PaymentNotFoundException("test-id"));

        mockMvc.perform(get("/api/payments/test-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found: test-id"));
    }

    @Test
    void shouldRefundPayment() throws Exception {
        Payment payment = new Payment(PaymentId.generate(), "order-1", Money.usd(100.00));
        when(refundPaymentUseCase.refund(any())).thenReturn(payment);

        mockMvc.perform(post("/api/payments/" + payment.getId() + "/refund"))
                .andExpect(status().isOk());
    }
}
