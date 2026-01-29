import {
  SagaOrchestrator,
  SagaExecutionResult,
  CompensationSummary,
  SagaStep,
  SagaContext,
  Order
} from '../../domain';

/**
 * Orchestrator for order fulfillment saga
 * Executes steps in sequence and handles compensation on failure
 */
export class OrderFulfillmentOrchestrator implements SagaOrchestrator {
  private readonly steps: ReadonlyArray<SagaStep>;

  constructor(steps: SagaStep[]) {
    this.steps = steps;
  }

  async execute(initialContext: SagaContext): Promise<SagaExecutionResult> {
    let context = initialContext;
    const completedSteps: string[] = [];
    const executedSteps: SagaStep[] = [];

    for (const step of this.steps) {
      const result = await step.execute(context);

      if (!result.success) {
        const compensationResults = await this.compensate(
          executedSteps,
          context
        );

        return {
          success: false,
          context,
          completedSteps,
          failedStep: step.name,
          error: result.error,
          compensationResults
        };
      }

      completedSteps.push(step.name);
      executedSteps.push(step);
      context = this.updateContext(context, step.name, result.data);
    }

    return {
      success: true,
      context,
      completedSteps
    };
  }

  getSteps(): ReadonlyArray<SagaStep> {
    return this.steps;
  }

  private async compensate(
    executedSteps: SagaStep[],
    context: SagaContext
  ): Promise<CompensationSummary> {
    if (executedSteps.length === 0) {
      return {
        triggered: false,
        completedCompensations: [],
        failedCompensations: []
      };
    }

    const completedCompensations: string[] = [];
    const failedCompensations: string[] = [];

    // Compensate in reverse order
    for (let i = executedSteps.length - 1; i >= 0; i--) {
      const step = executedSteps[i];
      const result = await step.compensate(context);

      if (result.success) {
        completedCompensations.push(step.name);
      } else {
        failedCompensations.push(step.name);
      }
    }

    return {
      triggered: true,
      completedCompensations,
      failedCompensations
    };
  }

  private updateContext(
    context: SagaContext,
    stepName: string,
    data: unknown
  ): SagaContext {
    const stepData = data as Record<string, unknown>;

    switch (stepName) {
      case 'CreateOrder':
        return context.withOrder(stepData.order as Order);
      case 'ReserveInventory':
        return context.withInventoryReservation(
          stepData.reservationId as string
        );
      case 'ProcessPayment':
        return context.withPaymentTransaction(
          stepData.transactionId as string
        );
      case 'ShipOrder':
        return context.withShipmentTracking(stepData.trackingId as string);
      default:
        return context;
    }
  }
}
