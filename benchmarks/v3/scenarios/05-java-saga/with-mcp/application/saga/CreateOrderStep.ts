import {
  SagaStep,
  StepResult,
  CompensationResult,
  SagaError,
  SagaContext,
  Order,
  OrderItem,
  OrderService
} from '../../domain';

interface CreateOrderResult {
  order: Order;
}

/**
 * Saga step for creating an order
 */
export class CreateOrderStep implements SagaStep<CreateOrderResult> {
  readonly name = 'CreateOrder';

  constructor(private readonly orderService: OrderService) {}

  async execute(context: SagaContext): Promise<StepResult<CreateOrderResult>> {
    try {
      const customerId = context.metadata['customerId'] as string;
      const items = context.metadata['items'] as OrderItem[];
      const totalAmount = context.metadata['totalAmount'] as number;

      const order = await this.orderService.createOrder(
        customerId,
        items,
        totalAmount
      );

      return {
        success: true,
        data: { order }
      };
    } catch (error) {
      return {
        success: false,
        error: new SagaError({
          message: `Failed to create order: ${(error as Error).message}`,
          code: 'ORDER_CREATION_FAILED',
          stepName: this.name,
          isRetryable: true
        })
      };
    }
  }

  async compensate(context: SagaContext): Promise<CompensationResult> {
    try {
      if (context.order) {
        await this.orderService.cancelOrder(context.order.id);
      }
      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: new SagaError({
          message: `Failed to cancel order: ${(error as Error).message}`,
          code: 'ORDER_CANCELLATION_FAILED',
          stepName: this.name,
          isRetryable: true
        })
      };
    }
  }
}
