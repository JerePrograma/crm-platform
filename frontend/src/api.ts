import type {
  AuditEvent,
  Credentials,
  DuplicateReview,
  Exclusion,
  ImportRow,
  ImportSummary,
  Page,
  Prospect,
  ProspectStatus,
} from "./types";

function authorization(credentials: Credentials): string {
  const bytes = new TextEncoder().encode(`${credentials.username}:${credentials.password}`);
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return `Basic ${btoa(binary)}`;
}

async function request<T>(
  path: string,
  credentials: Credentials,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Authorization", authorization(credentials));
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { ...init, headers });
  if (!response.ok) {
    const contentType = response.headers.get("content-type") ?? "";
    if (contentType.includes("application/problem+json")) {
      const problem = (await response.json()) as { title?: string; detail?: string };
      throw new Error(problem.detail ?? problem.title ?? `HTTP ${response.status}`);
    }
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function listProspects(
  credentials: Credentials,
  status?: ProspectStatus,
): Promise<Page<Prospect>> {
  const params = new URLSearchParams({ size: "100", sort: "createdAt,desc" });
  if (status) {
    params.set("status", status);
  }
  return request(`/api/v1/prospects?${params.toString()}`, credentials);
}

export function getProspect(credentials: Credentials, id: string): Promise<Prospect> {
  return request(`/api/v1/prospects/${id}`, credentials);
}

export function importProspects(
  credentials: Credentials,
  file: File,
  execute: boolean,
): Promise<ImportSummary> {
  const form = new FormData();
  form.set("file", file);
  const headers = new Headers();
  if (execute) {
    headers.set("X-Import-Confirmation", "EXECUTE_PROSPECT_IMPORT");
  }
  return request(
    `/api/v1/imports/prospects/${execute ? "execute" : "preview"}`,
    credentials,
    { method: "POST", headers, body: form },
  );
}

export function getImportRows(
  credentials: Credentials,
  jobId: string,
): Promise<ImportRow[]> {
  return request(`/api/v1/imports/prospects/${jobId}/rows`, credentials);
}

export function getPendingDuplicateReviews(
  credentials: Credentials,
): Promise<DuplicateReview[]> {
  return request("/api/v1/imports/prospects/duplicate-reviews/pending", credentials);
}

export function listExclusions(credentials: Credentials): Promise<Page<Exclusion>> {
  return request("/api/v1/exclusions?size=100&sort=createdAt,desc", credentials);
}

export function createExclusion(
  credentials: Credentials,
  input: { channelType: Exclusion["channelType"]; value: string; reason: string },
): Promise<Exclusion> {
  return request("/api/v1/exclusions", credentials, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function listAuditEvents(
  credentials: Credentials,
  limit = 100,
): Promise<AuditEvent[]> {
  return request(`/api/v1/audit?limit=${limit}`, credentials);
}
