import { CircuitBreaker, CircuitBreakerOptions } from './CircuitBreaker';

export interface HttpResponse<T = unknown> {
  status: number;
  data: T;
}

export interface HttpClientOptions extends Partial<CircuitBreakerOptions> {
  baseUrl?: string;
  timeout?: number;
}

export class HttpClient {
  private circuitBreaker: CircuitBreaker;
  private baseUrl: string;
  private requestTimeout: number;

  constructor(options: HttpClientOptions = {}) {
    this.circuitBreaker = new CircuitBreaker({
      failureThreshold: options.failureThreshold,
      successThreshold: options.successThreshold,
      timeout: options.timeout,
    });
    this.baseUrl = options.baseUrl ?? '';
    this.requestTimeout = options.timeout ?? 5000;
  }

  async get<T>(url: string): Promise<HttpResponse<T>> {
    return this.request<T>('GET', url);
  }

  async post<T>(url: string, body?: unknown): Promise<HttpResponse<T>> {
    return this.request<T>('POST', url, body);
  }

  async put<T>(url: string, body?: unknown): Promise<HttpResponse<T>> {
    return this.request<T>('PUT', url, body);
  }

  async delete<T>(url: string): Promise<HttpResponse<T>> {
    return this.request<T>('DELETE', url);
  }

  private async request<T>(method: string, url: string, body?: unknown): Promise<HttpResponse<T>> {
    const fullUrl = this.baseUrl + url;

    return this.circuitBreaker.execute(async () => {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), this.requestTimeout);

      try {
        const response = await fetch(fullUrl, {
          method,
          headers: {
            'Content-Type': 'application/json',
          },
          body: body ? JSON.stringify(body) : undefined,
          signal: controller.signal,
        });

        clearTimeout(timeoutId);

        if (!response.ok) {
          throw new Error(`HTTP error: ${response.status}`);
        }

        const data = await response.json() as T;
        return { status: response.status, data };
      } catch (error) {
        clearTimeout(timeoutId);
        throw error;
      }
    });
  }

  getCircuitState() {
    return this.circuitBreaker.getState();
  }

  resetCircuit() {
    this.circuitBreaker.reset();
  }
}
