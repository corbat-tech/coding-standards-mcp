package com.example.saga.application.saga.step;

import com.example.saga.application.port.PaymentService;
import com.example.saga.application.saga.CompensationResult;
import com.example.saga.application.saga.SagaContext;
import com.example.saga.application.saga.SagaStep;
import com.example.saga.application.saga.StepResult;
import com.example.saga.domain.entity.Order;
import com.example.saga.domain.exception.SagaException;

/**
 * Saga step for processing payment.
 */
public class ProcessPaymentStep implements SagaStep<String> {

    private static final String STEP_NAME = "ProcessPayment";

    private final PaymentService paymentService;

    public ProcessPaymentStep(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public String getName() {
        return STEP_NAME;
    }

    @Override
    public StepResult<String> execute(SagaContext context) {
        try {
            Order order = context.getOrder()
                    .orElseThrow(() -> new SagaException(
                            "Order not found in context",
                            "ORDER_NOT_FOUND",
                            STEP_NAME,
                            false
                    ));

            String transactionId = paymentService.processPayment(
                    context.getOrderId(),
                    order.getCustomerId(),
                    order.getTotalAmount()
            );

            return StepResult.success(transactionId);
        } catch (SagaException e) {
            return StepResult.failure(e);
        } catch (Exception e) {
            return StepResult.failure(new SagaException(
                    "Failed to process payment: " + e.getMessage(),
                    "PAYMENT_PROCESSING_FAILED",
                    STEP_NAME,
                    true,
                    e
            ));
        }
    }

    @Override
    public CompensationResult compensate(SagaContext context) {
        try {
            context.getPaymentTransactionId().ifPresent(
                    paymentService::refundPayment
            );
            return CompensationResult.success();
        } catch (Exception e) {
            return CompensationResult.failure(new SagaException(
                    "Failed to refund payment: " + e.getMessage(),
                    "PAYMENT_REFUND_FAILED",
                    STEP_NAME,
                    true,
                    e
            ));
        }
    }
}
