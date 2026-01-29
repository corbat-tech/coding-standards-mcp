import {
  SagaStep,
  StepResult,
  CompensationResult,
  SagaError,
  SagaContext,
  InventoryService
} from '../../domain';

interface ReserveInventoryResult {
  reservationId: string;
}

/**
 * Saga step for reserving inventory
 */
export class ReserveInventoryStep implements SagaStep<ReserveInventoryResult> {
  readonly name = 'ReserveInventory';

  constructor(private readonly inventoryService: InventoryService) {}

  async execute(
    context: SagaContext
  ): Promise<StepResult<ReserveInventoryResult>> {
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

      const reservationId = await this.inventoryService.reserveInventory(
        context.orderId,
        [...context.order.items]
      );

      return {
        success: true,
        data: { reservationId }
      };
    } catch (error) {
      return {
        success: false,
        error: new SagaError({
          message: `Failed to reserve inventory: ${(error as Error).message}`,
          code: 'INVENTORY_RESERVATION_FAILED',
          stepName: this.name,
          isRetryable: true
        })
      };
    }
  }

  async compensate(context: SagaContext): Promise<CompensationResult> {
    try {
      if (context.inventoryReservationId) {
        await this.inventoryService.releaseInventory(
          context.inventoryReservationId
        );
      }
      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: new SagaError({
          message: `Failed to release inventory: ${(error as Error).message}`,
          code: 'INVENTORY_RELEASE_FAILED',
          stepName: this.name,
          isRetryable: true
        })
      };
    }
  }
}
