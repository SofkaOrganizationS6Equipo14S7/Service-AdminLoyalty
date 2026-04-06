import { ApiClientConfig, ApiError, ApiErrorPayload } from "./types";

type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

interface RequestOptions {
  method: HttpMethod;
  path: string;
  query?: Record<string, string | number | boolean | undefined | null>;
  body?: unknown;
  requireAuth?: boolean;
}

export class HttpClient {
  private readonly config: ApiClientConfig;

  constructor(config: ApiClientConfig) {
    this.config = config;
  }

  public async get<T>(path: string, query?: RequestOptions["query"], requireAuth = true): Promise<T> {
    return this.request<T>({ method: "GET", path, query, requireAuth });
  }

  public async post<T>(path: string, body?: unknown, requireAuth = true): Promise<T> {
    return this.request<T>({ method: "POST", path, body, requireAuth });
  }

  public async put<T>(path: string, body?: unknown, requireAuth = true): Promise<T> {
    return this.request<T>({ method: "PUT", path, body, requireAuth });
  }

  public async patch<T>(path: string, body?: unknown, requireAuth = true): Promise<T> {
    return this.request<T>({ method: "PATCH", path, body, requireAuth });
  }

  public async delete(path: string, requireAuth = true): Promise<void> {
    await this.request<void>({ method: "DELETE", path, requireAuth });
  }

  private async request<T>(options: RequestOptions): Promise<T> {
    const url = this.buildUrl(options.path, options.query);
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
    };

    if (options.requireAuth) {
      const token = this.config.getToken?.();
      if (!token) {
        throw new ApiError(401, "No active session token available");
      }
      headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(url, {
      method: options.method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });

    if (response.status === 204) {
      return undefined as T;
    }

    const responseText = await response.text();
    const payload = this.tryParseJson(responseText);

    if (!response.ok) {
      const message =
        (payload && typeof payload.message === "string" && payload.message) ||
        response.statusText ||
        "Request failed";

      if (response.status === 401) {
        this.config.onUnauthorized?.();
      }

      throw new ApiError(response.status, message, payload);
    }

    return (payload as T) ?? (undefined as T);
  }

  private buildUrl(path: string, query?: RequestOptions["query"]): string {
    const normalizedBase = this.config.baseUrl.replace(/\/+$/, "");
    const normalizedPath = path.startsWith("/") ? path : `/${path}`;
    const url = new URL(`${normalizedBase}${normalizedPath}`);

    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null && `${value}`.length > 0) {
          url.searchParams.set(key, String(value));
        }
      });
    }

    return url.toString();
  }

  private tryParseJson(input: string): ApiErrorPayload | null {
    if (!input) {
      return null;
    }

    try {
      return JSON.parse(input) as ApiErrorPayload;
    } catch {
      return { message: input };
    }
  }
}

