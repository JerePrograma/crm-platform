import type {
  AuditEvent,
  AudienceRecipient,
  Campaign,
  CampaignChannel,
  CampaignSimulation,
  CampaignSequenceStep,
  DuplicateResolutionAction,
  DuplicateReview,
  Exclusion,
  ImportRow,
  ImportSummary,
  MessageTemplate,
  Opportunity,
  OpportunityStage,
  PipelineMetrics,
  Page,
  Prospect,
  ProspectStatus,
  Contact,
  Task,
  TimelineItem,
  SessionUser,
  RenderedTemplate,
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

export function listProspects(status?: ProspectStatus, query?: string): Promise<Page<Prospect>> {
  const params = new URLSearchParams({ size: "100", sort: "createdAt,desc" });
  if (status) {
    params.set("status", status);
  }
  if (query?.trim()) {
    params.set("query", query.trim());
  }
  return request(`/api/v1/prospects?${params.toString()}`);
}

export function createProspect(input: {
  institutionName: string;
  locality?: string;
  province?: string;
  country?: string;
  website?: string;
  source?: string;
}): Promise<Prospect> {
  return request("/api/v1/prospects", { method: "POST", body: JSON.stringify(input) });
}

export function updateProspect(id: string, input: Partial<Prospect> & { version: number; displayName: string }): Promise<Prospect> {
  return request(`/api/v1/prospects/${id}`, { method: "PUT", body: JSON.stringify(input) });
}

export function transitionProspect(id: string, version: number, status: ProspectStatus): Promise<Prospect> {
  return request(`/api/v1/prospects/${id}/transitions`, {
    method: "POST",
    body: JSON.stringify({
      version,
      status,
      reason: status === "LOST" ? "OTHER" : null,
      comment: status === "PROPOSAL" ? "Excepción manual documentada desde CRM" : "Cambio manual desde CRM",
      scheduledAt: status === "DEMO_SCHEDULED" ? new Date().toISOString() : null,
      proposalException: status === "PROPOSAL",
    }),
  });
}

export function listContacts(prospectId: string): Promise<Contact[]> {
  return request(`/api/v1/prospects/${prospectId}/contacts`);
}

export function createContact(
  prospectId: string,
  input: { firstName: string; lastName?: string; role?: string; email?: string },
): Promise<Contact> {
  const channels = input.email
    ? [{ type: "EMAIL", value: input.email, primary: true, valid: true, verified: false, consent: "UNKNOWN", preferred: true }]
    : [];
  return request(`/api/v1/prospects/${prospectId}/contacts`, {
    method: "POST",
    body: JSON.stringify({
      firstName: input.firstName,
      lastName: input.lastName,
      role: input.role,
      primary: true,
      verified: false,
      preferredChannel: input.email ? "EMAIL" : null,
      consent: "UNKNOWN",
      source: "MANUAL",
      channels,
    }),
  });
}

export function createNote(prospectId: string, body: string): Promise<void> {
  return request(`/api/v1/prospects/${prospectId}/notes`, {
    method: "POST",
    body: JSON.stringify({ body }),
  });
}

export function createTask(
  prospectId: string,
  input: { ownerUserId: string; title: string; dueAt: string },
): Promise<Task> {
  return request(`/api/v1/prospects/${prospectId}/tasks`, {
    method: "POST",
    body: JSON.stringify({ ...input, priority: "MEDIUM", taskType: "FOLLOW_UP" }),
  });
}

export function changeTaskStatus(task: Task, status: Task["status"]): Promise<Task> {
  return request(`/api/v1/tasks/${task.id}/status`, {
    method: "POST",
    body: JSON.stringify({ version: task.version, status }),
  });
}

export function listTasks(status?: Task["status"]): Promise<Task[]> {
  return request(`/api/v1/tasks${status ? `?status=${status}` : ""}`);
}

export function getTimeline(prospectId: string): Promise<Page<TimelineItem>> {
  return request(`/api/v1/prospects/${prospectId}/timeline?size=100`);
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
  return request("/api/v1/duplicate-reviews");
}

export function resolveDuplicateReview(
  reviewId: string,
  input: {
    action: DuplicateResolutionAction;
    survivorProspectId?: string;
    absorbedProspectId?: string;
    separateName?: string;
    comment?: string;
    idempotencyKey: string;
  },
): Promise<void> {
  return request(`/api/v1/duplicate-reviews/${reviewId}/resolution`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function listOpportunities(prospectId?: string): Promise<Opportunity[]> {
  const params = prospectId ? `?prospectId=${encodeURIComponent(prospectId)}` : "";
  return request(`/api/v1/opportunities${params}`);
}

export function getPipelineMetrics(): Promise<PipelineMetrics> {
  return request("/api/v1/opportunities/metrics");
}

export function createOpportunity(input: {
  prospectId: string;
  name: string;
  ownerId: string;
  estimatedValue: number;
  currency: string;
  probability: number;
  expectedCloseDate?: string;
  source?: string;
  primaryActive: boolean;
}): Promise<Opportunity> {
  return request("/api/v1/opportunities", {
    method: "POST",
    body: JSON.stringify({ version: 0, ...input }),
  });
}

export function transitionOpportunity(
  id: string,
  version: number,
  stage: OpportunityStage,
  reason?: string,
): Promise<Opportunity> {
  return request(`/api/v1/opportunities/${id}/transitions`, {
    method: "POST",
    body: JSON.stringify({ version, stage, reason, comment: "Transición desde pipeline" }),
  });
}

export function listTemplates(): Promise<MessageTemplate[]> {
  return request("/api/v1/templates");
}

export function createTemplate(input: {
  name: string;
  channel: CampaignChannel;
  subject: string;
  textBody: string;
  htmlBody: string;
}): Promise<MessageTemplate> {
  return request("/api/v1/templates", { method: "POST", body: JSON.stringify(input) });
}

export function previewTemplate(
  versionId: string,
  variables: Record<string, string>,
): Promise<RenderedTemplate> {
  return request(`/api/v1/template-versions/${versionId}/preview`, {
    method: "POST",
    body: JSON.stringify(variables),
  });
}

export function listCampaigns(): Promise<Campaign[]> {
  return request("/api/v1/campaigns");
}

export function createCampaign(input: {
  name: string;
  description?: string;
  objective?: string;
  channel: CampaignChannel;
  templateVersionId: string;
}): Promise<Campaign> {
  return request("/api/v1/campaigns", { method: "POST", body: JSON.stringify(input) });
}

export function freezeCampaignAudience(
  campaign: Campaign,
  filter: { province?: string; scoreAtLeast?: number },
): Promise<Campaign> {
  return request(`/api/v1/campaigns/${campaign.id}/audience/freeze`, {
    method: "POST",
    body: JSON.stringify({
      version: campaign.version,
      eligibility: "ELIGIBLE",
      excludeCustomers: true,
      requireActiveOpportunity: false,
      ...filter,
    }),
  });
}

export function getCampaignAudience(id: string): Promise<AudienceRecipient[]> {
  return request(`/api/v1/campaigns/${id}/audience`);
}

export function approveCampaign(campaign: Campaign): Promise<Campaign> {
  return request(`/api/v1/campaigns/${campaign.id}/approve`, {
    method: "POST",
    body: JSON.stringify({ version: campaign.version }),
  });
}

export function simulateCampaign(campaign: Campaign): Promise<CampaignSimulation> {
  return request(`/api/v1/campaigns/${campaign.id}/simulate`, {
    method: "POST",
    headers: { "Idempotency-Key": crypto.randomUUID() },
  });
}

export function replaceCampaignSequence(campaign: Campaign): Promise<CampaignSequenceStep[]> {
  return request(`/api/v1/campaigns/${campaign.id}/sequence`, {
    method: "PUT",
    body: JSON.stringify({
      version: campaign.version,
      steps: [
        { type: campaign.channel, configuration: {} },
        { type: "WAIT", configuration: { days: 2 } },
        { type: "CONDITION", configuration: { condition: "REPLIED", action: "STOP" } },
        { type: "STOP", configuration: {} },
      ],
    }),
  });
}

export function getCampaignSequence(id: string): Promise<CampaignSequenceStep[]> {
  return request(`/api/v1/campaigns/${id}/sequence`);
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
