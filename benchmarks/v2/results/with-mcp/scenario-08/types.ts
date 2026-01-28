export type CircuitState = 'closed' | 'open' | 'half-open';

export interface CircuitBreakerConfig {
  failureThreshold: number;
  successThreshold: number;
  timeout: number;
}

export interface CircuitBreakerMetrics {
  state: CircuitState;
  failures: number;
  successes: number;
  lastFailureTime: number | null;
  totalRequests: number;
  totalFailures: number;
  totalSuccesses: number;
}

export class CircuitOpenError extends Error {
  constructor(public readonly retryAfter: number) {
    super(`Circuit is open. Retry after ${retryAfter}ms`);
    this.name = 'CircuitOpenError';
  }
}
