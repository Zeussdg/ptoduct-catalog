const BASE_URL = import.meta.env.VITE_API_URL || "/api";

export class ApiClientError extends Error {
  constructor(status, code, message, details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

async function request(path, { method = "GET", body, headers, isFormData } = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    credentials: "include",
    headers: isFormData
      ? headers
      : { "Content-Type": "application/json", ...(headers || {}) },
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
  });

  if (res.status === 204) return null;

  const isJson = res.headers.get("content-type")?.includes("application/json");
  const payload = isJson ? await res.json() : null;

  if (!res.ok) {
    const err = payload?.error;
    throw new ApiClientError(res.status, err?.code ?? "UNKNOWN", err?.message ?? "Bir hata oluştu", err?.details);
  }

  return payload;
}

export const apiClient = {
  get: (path) => request(path),
  post: (path, body, opts) => request(path, { method: "POST", body, ...opts }),
  put: (path, body, opts) => request(path, { method: "PUT", body, ...opts }),
  delete: (path) => request(path, { method: "DELETE" }),
};
