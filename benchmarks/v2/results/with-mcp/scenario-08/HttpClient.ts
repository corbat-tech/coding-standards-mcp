export interface HttpResponse<T> {
  status: number;
  data: T;
}

export interface HttpClient {
  get<T>(url: string): Promise<HttpResponse<T>>;
  post<T>(url: string, body: unknown): Promise<HttpResponse<T>>;
}

export class HttpRequestError extends Error {
  constructor(
    public readonly url: string,
    public readonly status: number,
    message: string
  ) {
    super(message);
    this.name = 'HttpRequestError';
  }
}
