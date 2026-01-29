import {
  SagaStep,
  StepResult,
  CompensationResult,
  SagaError,
  SagaContext,
  PaymentService
} from '../../domain';

interface ProcessPaymentResult {
  transactionId: string;
}

/**
 * Saga step for processing payment
 */
export class ProcessPaymentStep implements SagaStep<ProcessPaymentResult> {
  readonly name = 'ProcessPayment';

  constructor(private readonly paymentService: PaymentService) {}

  async execute(
    context: SagaContext
  ): Promise<StepResult<ProcessPaymentResult>> {
    try {
      if (!context.order) {
        return {
          success: false,
          error: new SagaError({
            message: 'Order not found in context',
            code: 'ORDER_NOT_FOUND',
            stepName: this.name,
            isRetryable: false
          })
        };
      }

      const transactionId = await this.paymentService.processPayment(
        context.orderId,
        context.order.customerId,
        context.order.totalAmount
      );

      return {
        success: true,
        data: { transactionId }
      };
    } catch (error) {
      return {
        success: false,
        error: new SagaError({
          message: `Failed to process payment: ${(error as Error).message}`,
          code: 'PAYMENT_PROCESSING_FAILED',
          stepName: this.name,
          isRetryable: true
        })
      };
    }
  }

  async compensate(context: SagaContext): Promise<CompensationResult> {
    try {
      if (context.paymentTransactionId) {
        await this.paymentService.refundPayment(context.paymentTransactionId);
      }
      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: new SagaError({
          message: `Failed to refund payment: ${(error as Error).message}`,
          code: 'PAYMENT_REFUND_FAILED',
          stepName: this.name,
          isRetryable: true
        })
      };
    }
  }
}
