import {
  CircuitState,
  CircuitBreakerConfig,
  CircuitBreakerMetrics,
  CircuitOpenError,
} from './types';
import { Clock } from './Clock';

const DEFAULT_CONFIG: CircuitBreakerConfig = {
  failureThreshold: 5,
  successThreshold: 2,
  timeout: 30000,
};

export class CircuitBreaker {
  private state: CircuitState = 'closed';
  private failures = 0;
  private successes = 0;
  private lastFailureTime: number | null = null;
  private totalRequests = 0;
  private totalFailures = 0;
  private totalSuccesses = 0;

  private readonly config: CircuitBreakerConfig;
  private readonly clock: Clock;

  constructor(config: Partial<CircuitBreakerConfig> = {}, clock: Clock) {
    this.config = { ...DEFAULT_CONFIG, ...config };
    this.clock = clock;
  }

  async execute<T>(fn: () => Promise<T>): Promise<T> {
    this.totalRequests++;

    if (this.isOpen()) {
      throw new CircuitOpenError(this.getRetryAfter());
    }

    try {
      const result = await fn();
      this.onSuccess();
      return result;
    } catch (error) {
      this.onFailure();
      throw error;
    }
  }

  getState(): CircuitState {
    if (this.state === 'open' && this.shouldAttemptReset()) {
      return 'half-open';
    }
    return this.state;
  }

  getMetrics(): CircuitBreakerMetrics {
    return {
      state: this.getState(),
      failures: this.failures,
      successes: this.successes,
      lastFailureTime: this.lastFailureTime,
      totalRequests: this.totalRequests,
      totalFailures: this.totalFailures,
      totalSuccesses: this.totalSuccesses,
    };
  }

  reset(): void {
    this.state = 'closed';
    this.failures = 0;
    this.successes = 0;
    this.lastFailureTime = null;
  }

  private isOpen(): boolean {
    if (this.state === 'closed') {
      return false;
    }

    if (this.state === 'open' && this.shouldAttemptReset()) {
      this.state = 'half-open';
      return false;
    }

    return this.state === 'open';
  }

  private shouldAttemptReset(): boolean {
    if (this.lastFailureTime === null) {
      return false;
    }
    return this.clock.now() - this.lastFailureTime >= this.config.timeout;
  }

  private getRetryAfter(): number {
    if (this.lastFailureTime === null) {
      return 0;
    }
    const elapsed = this.clock.now() - this.lastFailureTime;
    return Math.max(0, this.config.timeout - elapsed);
  }

  private onSuccess(): void {
    this.totalSuccesses++;

    if (this.state === 'half-open') {
      this.successes++;
      if (this.successes >= this.config.successThreshold) {
        this.transitionToClosed();
      }
    } else {
      this.failures = 0;
    }
  }

  private onFailure(): void {
    this.totalFailures++;
    this.failures++;
    this.lastFailureTime = this.clock.now();

    if (this.state === 'half-open') {
      this.transitionToOpen();
    } else if (this.failures >= this.config.failureThreshold) {
      this.transitionToOpen();
    }
  }

  private transitionToOpen(): void {
    this.state = 'open';
    this.successes = 0;
  }

  private transitionToClosed(): void {
    this.state = 'closed';
    this.failures = 0;
    this.successes = 0;
  }
}
