import {
  SagaStep,
  StepResult,
  CompensationResult,
  SagaError,
  SagaContext,
  ShippingService
} from '../../domain';

interface ShipOrderResult {
  trackingId: string;
}

/**
 * Saga step for shipping order
 */
export class ShipOrderStep implements SagaStep<ShipOrderResult> {
  readonly name = 'ShipOrder';

  constructor(private readonly shippingService: ShippingService) {}

  async execute(context: SagaContext): Promise<StepResult<ShipOrderResult>> {
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

      const trackingId = await this.shippingService.createShipment(
        context.orderId,
        context.order.customerId
      );

      return {
        success: true,
        data: { trackingId }
      };
    } catch (error) {
      return {
        success: false,
        error: new SagaError({
          message: `Failed to ship order: ${(error as Error).message}`,
          code: 'SHIPPING_FAILED',
          stepName: this.name,
          isRetryable: true
        })
      };
    }
  }

  async compensate(context: SagaContext): Promise<CompensationResult> {
    try {
      if (context.shipmentTrackingId) {
        await this.shippingService.cancelShipment(context.shipmentTrackingId);
      }
      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: new SagaError({
          message: `Failed to cancel shipment: ${(error as Error).message}`,
          code: 'SHIPMENT_CANCELLATION_FAILED',
          stepName: this.name,
          isRetryable: true
        })
      };
    }
  }
}
