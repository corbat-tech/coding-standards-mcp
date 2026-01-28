import { CircuitBreaker } from './CircuitBreaker';
import { HttpClient, HttpResponse } from './HttpClient';
import { CircuitBreakerConfig } from './types';
import { Clock, SystemClock } from './Clock';

export class ResilientHttpClient implements HttpClient {
  private readonly circuitBreaker: CircuitBreaker;
  private readonly delegate: HttpClient;

  constructor(
    delegate: HttpClient,
    config?: Partial<CircuitBreakerConfig>,
    clock: Clock = new SystemClock()
  ) {
    this.delegate = delegate;
    this.circuitBreaker = new CircuitBreaker(config, clock);
  }

  async get<T>(url: string): Promise<HttpResponse<T>> {
    return this.circuitBreaker.execute(() => this.delegate.get<T>(url));
  }

  async post<T>(url: string, body: unknown): Promise<HttpResponse<T>> {
    return this.circuitBreaker.execute(() => this.delegate.post<T>(url, body));
  }

  getCircuitState() {
    return this.circuitBreaker.getState();
  }

  getMetrics() {
    return this.circuitBreaker.getMetrics();
  }

  resetCircuit() {
    this.circuitBreaker.reset();
  }
}
