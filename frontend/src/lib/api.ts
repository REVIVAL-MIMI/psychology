import { clearStoredAuth, getStoredAuth, setStoredAuth } from "./storage";

const API_PREFIX = "/api/v1";

export class ApiError extends Error {
  status: number;
  payload?: unknown;
  constructor(message: string, status: number, payload?: unknown) {
    super(message);
    this.status = status;
    this.payload = payload;
  }
}

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  headers?: Record<string, string>;
  isForm?: boolean;
  skipAuth?: boolean;
};

async function request<T>(path: string, options: RequestOptions = {}, retry = true): Promise<T> {
  const auth = getStoredAuth();
  const headers: Record<string, string> = {
    ...(options.isForm ? {} : { "Content-Type": "application/json" }),
    ...(options.headers ?? {})
  };

  if (!options.skipAuth && auth?.accessToken) {
    headers.Authorization = `Bearer ${auth.accessToken}`;
  }

  const response = await fetch(`${API_PREFIX}${path}`,
    {
      method: options.method ?? "GET",
      headers,
      body: options.body ? (options.isForm ? (options.body as BodyInit) : JSON.stringify(options.body)) : undefined,
      credentials: "include"
    }
  );

  if (response.status === 401 && retry) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      return request<T>(path, options, false);
    }
  }

  if (!response.ok) {
    let payload: unknown = undefined;
    try {
      payload = await response.json();
    } catch {
      payload = await response.text();
    }
    throw new ApiError(`Request failed: ${response.status}`, response.status, payload);
  }

  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    return response.json() as Promise<T>;
  }
  return (await response.text()) as unknown as T;
}

async function tryRefresh(): Promise<boolean> {
  try {
    const response = await fetch(`${API_PREFIX}/auth/refresh`, {
      method: "POST",
      credentials: "include"
    });
    if (!response.ok) return false;
    const data = (await response.json()) as {
      accessToken: string;
      userId: number;
      userRole: "ROLE_CLIENT" | "ROLE_PSYCHOLOGIST" | "ROLE_ADMIN";
      fullName: string;
      phone: string;
    };
    setStoredAuth(data);
    return true;
  } catch {
    clearStoredAuth();
    return false;
  }
}

export const api = {
  get: <T = any>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "GET" }),
  post: <T = any>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "POST", body }),
  put: <T = any>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PUT", body }),
  del: <T = any>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "DELETE" }),
  upload: <T = any>(path: string, formData: FormData) =>
    request<T>(path, { method: "POST", body: formData, isForm: true })
};
