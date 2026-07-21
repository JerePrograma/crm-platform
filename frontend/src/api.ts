import type {
  AuditEvent,
  DuplicateReview,
  Exclusion,
  ImportRow,
  ImportSummary,
  Page,
  Prospect,
  ProspectStatus,
  SessionUser,
  User,
} from "./types";

type Csrf = { token: string; headerName: string };

let csrfRequest: Promise<Csrf> | null = null;

async function csrf(): Promise<Csrf> {
  csrfRequest ??= fetch("/api/v1/auth/csrf", { credentials: "same-origin" }).then(
    async (response) => {
      if (!response.ok) {
        throw new Error(`No se pudo iniciar la sesión segura (HTTP ${response.status})`);
      }
      return (await response.json()) as Csrf;
    },
  );
  return csrfRequest;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const method = (init.method ?? "GET").toUpperCase();
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const token = await csrf();
    headers.set(token.headerName, token.token);
  }
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { ...init, headers, credentials: "same-origin" });
  if (!response.ok) {
    const contentType = response.headers.get("content-type") ?? "";
    if (contentType.includes("application/problem+json")) {
      const problem = (await response.json()) as { title?: string; detail?: string };
      throw new Error(problem.detail ?? problem.title ?? `HTTP ${response.status}`);
    }
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  if (
    response.status === 204 ||
    response.headers.get("content-length") === "0" ||
    !response.headers.get("content-type")
  ) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function login(username: string, password: string): Promise<SessionUser> {
  return request("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

export function getSession(): Promise<SessionUser> {
  return request("/api/v1/auth/me");
}

export async function logout(): Promise<void> {
  await request("/api/v1/auth/logout", { method: "POST" });
  csrfRequest = null;
}

export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return request("/api/v1/auth/password", {
    method: "POST",
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

export function listUsers(): Promise<User[]> {
  return request("/api/v1/users");
}

export function createUser(input: {
  username: string;
  displayName: string;
  password: string;
  role: User["role"];
}): Promise<User> {
  return request("/api/v1/users", { method: "POST", body: JSON.stringify(input) });
}

export function setUserActive(id: string, active: boolean): Promise<void> {
  return request(`/api/v1/users/${id}/active`, {
    method: "PATCH",
    body: JSON.stringify({ active }),
  });
}

export function listProspects(status?: ProspectStatus): Promise<Page<Prospect>> {
  const params = new URLSearchParams({ size: "100", sort: "createdAt,desc" });
  if (status) {
    params.set("status", status);
  }
  return request(`/api/v1/prospects?${params.toString()}`);
}

export function getProspect(id: string): Promise<Prospect> {
  return request(`/api/v1/prospects/${id}`);
}

export function importProspects(file: File, execute: boolean): Promise<ImportSummary> {
  const form = new FormData();
  form.set("file", file);
  const headers = new Headers();
  if (execute) {
    headers.set("X-Import-Confirmation", "EXECUTE_PROSPECT_IMPORT");
  }
  return request(`/api/v1/imports/prospects/${execute ? "execute" : "preview"}`, {
    method: "POST",
    headers,
    body: form,
  });
}

export function getImportRows(jobId: string): Promise<ImportRow[]> {
  return request(`/api/v1/imports/prospects/${jobId}/rows`);
}

export function getPendingDuplicateReviews(): Promise<DuplicateReview[]> {
  return request("/api/v1/imports/prospects/duplicate-reviews/pending");
}

export function listExclusions(): Promise<Page<Exclusion>> {
  return request("/api/v1/exclusions?size=100&sort=createdAt,desc");
}

export function createExclusion(input: {
  channelType: Exclusion["channelType"];
  value: string;
  reason: string;
}): Promise<Exclusion> {
  return request("/api/v1/exclusions", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function listAuditEvents(limit = 100): Promise<AuditEvent[]> {
  return request(`/api/v1/audit?limit=${limit}`);
}
