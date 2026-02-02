package com.example.saga.application.saga.step;

import com.example.saga.application.port.PaymentService;
import com.example.saga.application.saga.*;
import com.example.saga.domain.entity.Order;

public class ProcessPaymentStep implements SagaStep {

    private static final String PAYMENT_ID_KEY = "paymentId";
    private final PaymentService paymentService;

    public ProcessPaymentStep(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public String getName() { return "ProcessPayment"; }

    @Override
    public StepResult execute(SagaContext context) {
        try {
            Order order = context.getOrder();
            String paymentId = paymentService.processPayment(
                    order.getId(), order.getCustomerId(), order.getTotalAmount());
            context.put(PAYMENT_ID_KEY, paymentId);
            return StepResult.success();
        } catch (Exception e) {
            return StepResult.failure(e.getMessage());
        }
    }

    @Override
    public void compensate(SagaContext context) {
        if (context.has(PAYMENT_ID_KEY)) {
            paymentService.refundPayment(context.get(PAYMENT_ID_KEY));
        }
    }
}
