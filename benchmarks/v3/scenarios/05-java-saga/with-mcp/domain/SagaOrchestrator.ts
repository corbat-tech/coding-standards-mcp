import { SagaContext } from './SagaContext';
import { SagaStep } from './SagaStep';

/**
 * Result of a saga execution
 */
export interface SagaExecutionResult {
  readonly success: boolean;
  readonly context: SagaContext;
  readonly completedSteps: string[];
  readonly failedStep?: string;
  readonly error?: Error;
  readonly compensationResults?: CompensationSummary;
}

export interface CompensationSummary {
  readonly triggered: boolean;
  readonly completedCompensations: string[];
  readonly failedCompensations: string[];
}

/**
 * Interface for saga orchestrator
 */
export interface SagaOrchestrator {
  /**
   * Execute the saga with all registered steps
   * @param context Initial saga context
   * @returns Result of the saga execution
   */
  execute(context: SagaContext): Promise<SagaExecutionResult>;

  /**
   * Get the list of registered steps
   */
  getSteps(): ReadonlyArray<SagaStep>;
}
