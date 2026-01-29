import { SagaContext } from './SagaContext';

/**
 * Interface for a saga step with execute and compensate operations
 */
export interface SagaStep<T = unknown> {
  readonly name: string;

  /**
   * Execute the saga step
   * @param context The saga context containing shared state
   * @returns Result of the step execution
   */
  execute(context: SagaContext): Promise<StepResult<T>>;

  /**
   * Compensate (rollback) the saga step
   * @param context The saga context containing shared state
   * @returns Result of the compensation
   */
  compensate(context: SagaContext): Promise<CompensationResult>;
}

export interface StepResult<T = unknown> {
  readonly success: boolean;
  readonly data?: T;
  readonly error?: SagaError;
}

export interface CompensationResult {
  readonly success: boolean;
  readonly error?: SagaError;
}

export class SagaError extends Error {
  readonly code: string;
  readonly stepName: string;
  readonly isRetryable: boolean;

  constructor(params: {
    message: string;
    code: string;
    stepName: string;
    isRetryable?: boolean;
  }) {
    super(params.message);
    this.name = 'SagaError';
    this.code = params.code;
    this.stepName = params.stepName;
    this.isRetryable = params.isRetryable ?? false;
  }
}

export class CompensationError extends Error {
  readonly originalError: SagaError;
  readonly failedCompensations: string[];

  constructor(params: {
    message: string;
    originalError: SagaError;
    failedCompensations: string[];
  }) {
    super(params.message);
    this.name = 'CompensationError';
    this.originalError = params.originalError;
    this.failedCompensations = params.failedCompensations;
  }
}
