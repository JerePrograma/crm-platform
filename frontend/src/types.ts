export type ProspectStatus =
  | "NEW"
  | "QUALIFYING"
  | "READY_TO_CONTACT"
  | "FOLLOW_UP"
  | "DEMO_PROPOSED"
  | "DEMO_SCHEDULED"
  | "PROPOSAL"
  | "CUSTOMER"
  | "NEEDS_ENRICHMENT"
  | "READY_FOR_REVIEW"
  | "APPROVED"
  | "QUEUED"
  | "CONTACTED"
  | "REPLIED"
  | "INTERESTED"
  | "QUALIFIED"
  | "TRIAL_PROPOSED"
  | "TRIAL_ACTIVE"
  | "QUOTED"
  | "NEGOTIATION"
  | "WON"
  | "LOST"
  | "NO_RESPONSE"
  | "BOUNCED"
  | "UNSUBSCRIBED"
  | "DO_NOT_CONTACT"
  | "INVALID"
  | "DUPLICATE"
  | "ARCHIVED";

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
};

export type CurrencyTotal = {
  currency: string;
  opportunityCount: number;
  totalValue: number;
  weightedValue: number;
};

export type DashboardReport = {
  from: string;
  to: string;
  timezone: string;
  prospectsByStatus: Record<string, number>;
  prospectsBySource: Record<string, number>;
  prospectsByOwner: Record<string, number>;
  prospectSummary: Record<string, number>;
  tasks: Record<string, number>;
  activitiesByChannel: Record<string, number>;
  activityOutcomes: Record<string, number>;
  opportunitiesByStage: Record<string, number>;
  opportunityValues: CurrencyTotal[];
  opportunitySummary: Record<string, number>;
  lossReasons: Record<string, number>;
  campaigns: Record<string, number>;
  outbox: Record<string, number>;
  inbound: Record<string, number>;
  operations: Record<string, number>;
};

export type OrganizationSettings = {
  version: number;
  name: string;
  timezone: string;
  currency: string;
  locale: string;
  brandingPrimaryColor: string;
  followUpDays: number;
  operatingWindowStart: string;
  operatingWindowEnd: string;
  businessDays: number[];
  campaignDailyLimit: number;
  sendingOverrideRejected: boolean;
  sending: {
    environmentEnabled: boolean;
    environmentDryRun: boolean;
    environmentDailyLimit: number;
    environmentKillSwitch: boolean;
    databaseEnabled: string;
    databaseDryRun: string;
    databaseDailyLimit: number;
    databaseKillSwitch: string;
  };
};

export type CrmTag = {
  id: string;
  version: number;
  name: string;
  color: string;
  active: boolean;
  usageCount: number;
  createdAt: string;
  updatedAt: string;
};

export type Prospect = {
  id: string;
  version: number;
  institutionId: string;
  displayName: string;
  legalName: string | null;
  province: string | null;
  country: string | null;
  website: string | null;
  status: ProspectStatus;
  priority: number | null;
  score: number | null;
  estimatedStudents: number | null;
  source: string | null;
  sourceDetail: string | null;
  ownerUserId: string | null;
  ownerName: string | null;
  address: string | null;
  city: string | null;
  timezone: string | null;
  notesSummary: string | null;
  nextActionAt: string | null;
  lastContactAt: string | null;
  eligibility: "ELIGIBLE" | "EXCLUDED" | "CUSTOMER";
  lostReason: string | null;
  statusDetailAt: string | null;
  archivedAt: string | null;
  contactEligible: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ContactChannel = {
  id: string;
  version: number;
  type: "EMAIL" | "PHONE" | "WHATSAPP" | "WEBSITE" | "SOCIAL" | "OTHER";
  value: string;
  normalizedValue: string;
  primary: boolean;
  valid: boolean;
  verified: boolean;
  consent: "UNKNOWN" | "GRANTED" | "DENIED";
  preferred: boolean;
  lastValidatedAt: string | null;
};

export type Contact = {
  id: string;
  version: number;
  firstName: string | null;
  lastName: string | null;
  displayName: string;
  role: string | null;
  primary: boolean;
  verified: boolean;
  preferredChannel: ContactChannel["type"] | null;
  consent: "UNKNOWN" | "GRANTED" | "DENIED";
  source: string | null;
  lastValidatedAt: string | null;
  createdAt: string;
  updatedAt: string;
  channels: ContactChannel[];
};

export type Task = {
  id: string;
  version: number;
  prospectId: string;
  ownerUserId: string;
  creatorUserId: string | null;
  title: string;
  description: string | null;
  dueAt: string;
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  status: "OPEN" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
  taskType: string;
  reminderAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  outcome: string | null;
  createdAt: string;
  updatedAt: string;
};

export type TimelineItem = {
  id: string;
  eventAt: string;
  eventType: "STATUS" | "NOTE" | "ACTIVITY" | "TASK" | "AUDIT";
  title: string;
  detail: string | null;
  actorUserId: string | null;
  metadata: string;
};

export type ImportSummary = {
  id: string;
  fileName: string;
  fileSha256: string;
  sourceType: "CSV" | "XLSX";
  dryRun: boolean;
  status: "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";
  totalRows: number;
  acceptedRows: number;
  excludedRows: number;
  rejectedRows: number;
  duplicateRows: number;
  reviewRows: number;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
};

export type ImportRow = {
  id: string;
  sourceSheet: string;
  rowNumber: number;
  status:
    | "PENDING"
    | "ACCEPTED"
    | "EXCLUDED"
    | "REJECTED"
    | "DUPLICATE"
    | "REVIEW_REQUIRED";
  normalizedEmail: string | null;
  normalizedPhone: string | null;
  errorMessage: string | null;
  prospectId: string | null;
};

export type DuplicateReview = {
  id: string;
  version: number;
  status: "PENDING" | "DEFERRED";
  matchType: string;
  confidence: number;
  matchReasons: string | null;
  existingProspectId: string | null;
  sourceSheet: string;
  rowNumber: number;
  sourceData: string;
  normalizedEmail: string | null;
  normalizedPhone: string | null;
  existingProspect: {
    id: string;
    displayName: string;
    locality: string | null;
    website: string | null;
    status: string;
  } | null;
  createdAt: string;
};

export type DuplicateResolutionAction =
  | "MARK_NOT_DUPLICATE"
  | "LINK_TO_EXISTING"
  | "MERGE"
  | "CREATE_SEPARATE"
  | "REJECT_ROW"
  | "DEFER";

export type Exclusion = {
  id: string;
  version: number;
  channelType: "EMAIL" | "PHONE" | "WHATSAPP" | "WEBSITE" | "SOCIAL";
  normalizedValue: string;
  reason: string;
  createdAt: string;
  updatedAt: string;
};

export type AuditEvent = {
  id: string;
  createdAt: string;
  action: string;
  entityType: string;
  entityId: string | null;
  payload: string;
};

export type SessionUser = {
  userId: string;
  organizationId: string;
  username: string;
  displayName: string;
  role: "ADMIN" | "MANAGER" | "SALES" | "VIEWER";
  permissions: string[];
};

export type User = {
  id: string;
  username: string;
  displayName: string;
  role: "ADMIN" | "MANAGER" | "SALES" | "VIEWER";
  active: boolean;
  createdAt: string;
  lastLoginAt: string | null;
};

export type OpportunityStage =
  | "QUALIFICATION"
  | "DISCOVERY"
  | "DEMO"
  | "PROPOSAL"
  | "NEGOTIATION"
  | "WON"
  | "LOST";

export type Opportunity = {
  id: string;
  version: number;
  prospectId: string;
  prospectName: string;
  name: string;
  ownerId: string;
  ownerName: string;
  stage: OpportunityStage;
  estimatedValue: number;
  currency: string;
  probability: number;
  expectedCloseDate: string | null;
  actualCloseDate: string | null;
  lostReason: string | null;
  wonReason: string | null;
  source: string | null;
  primaryActive: boolean;
  stageChangedAt: string;
  createdAt: string;
  updatedAt: string;
};

export type PipelineMetrics = {
  activeCount: number;
  totalValue: number;
  weightedValue: number;
  stalledCount: number;
  byStage: Record<OpportunityStage, number>;
};

export type CampaignChannel = "EMAIL" | "WHATSAPP";

export type CampaignState =
  | "DRAFT"
  | "READY_FOR_REVIEW"
  | "APPROVED"
  | "SIMULATED"
  | "SCHEDULED"
  | "RUNNING"
  | "PAUSED"
  | "COMPLETED"
  | "CANCELLED"
  | "FAILED";

export type MessageTemplate = {
  id: string;
  name: string;
  channel: CampaignChannel;
  versionId: string;
  versionNumber: number;
  subject: string;
  textBody: string;
  htmlBody: string;
  variables: string[];
  createdAt: string;
};

export type Campaign = {
  id: string;
  version: number;
  name: string;
  description: string | null;
  objective: string | null;
  channel: CampaignChannel;
  status: CampaignState;
  dryRun: boolean;
  dailyLimit: number;
  approved: boolean;
  templateVersionId: string;
  templateName: string;
  recipientCount: number;
  excludedCount: number;
  frozenAt: string | null;
  approvedAt: string | null;
  simulatedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AudienceRecipient = {
  prospectId: string;
  prospectName: string;
  contactId: string | null;
  contactName: string | null;
  included: boolean;
  exclusionReason: string | null;
  channel: CampaignChannel;
  validationStatus: "VALID" | "MISSING_CHANNEL" | "EXCLUDED" | "INELIGIBLE";
  frozenAt: string;
};

export type CampaignSimulation = {
  id: string;
  campaignId: string;
  idempotencyKey: string;
  includedCount: number;
  excludedCount: number;
  status: "SIMULATED";
  createdAt: string;
};

export type CampaignSequenceStep = {
  id: string;
  order: number;
  type: "EMAIL" | "WHATSAPP" | "MANUAL_TASK" | "WAIT" | "CONDITION" | "STOP";
  configuration: Record<string, unknown>;
};

export type RenderedTemplate = {
  subject: string;
  textBody: string;
  htmlBody: string;
};

export type MessagingSafety = {
  emailMode: string;
  whatsAppMode: string;
  realNetworkAllowed: boolean;
  selectedEmailProvider: string;
  selectedWhatsAppProvider: string;
  sendEndpointAvailable: boolean;
};

export type MessageRecord = {
  id: string;
  channel: CampaignChannel;
  status:
    | "DRAFT_CREATED"
    | "SIMULATED"
    | "PROVIDER_DRAFT_CREATED"
    | "BLOCKED_BY_KILL_SWITCH"
    | "BLOCKED_BY_CONFIGURATION"
    | "BLOCKED_BY_EXCLUSION"
    | "BLOCKED_BY_POLICY";
  sendingBlockReason: string;
  provider: string;
  externalMessageId: string | null;
  externalThreadId: string | null;
};

export type ManualMessageLink = {
  channel: CampaignChannel;
  status: "DRAFT_CREATED";
  url: string;
  sendingBlockReason: string;
};

export type OutboxStatus =
  | "PENDING"
  | "PROCESSING"
  | "SUCCEEDED"
  | "RETRY"
  | "DEAD"
  | "CANCELLED"
  | "BLOCKED";

export type OutboxEvent = {
  id: string;
  eventType: string;
  eventVersion: number;
  aggregateType: string;
  aggregateId: string;
  payload: string;
  status: OutboxStatus;
  attemptCount: number;
  maxAttempts: number;
  nextAttemptAt: string;
  lockedAt: string | null;
  lockExpiresAt: string | null;
  lockedBy: string | null;
  createdAt: string;
  updatedAt: string;
  processedAt: string | null;
  lastErrorCode: string | null;
  lastErrorSummary: string | null;
  resultSummary: string | null;
  idempotencyKey: string;
  correlationId: string;
};

export type OutboxMetric = {
  status: OutboxStatus;
  count: number;
  oldestCreatedAt: string | null;
};

export type WorkerState = {
  worker: {
    workerId: string;
    schedulerEnabled: boolean;
    acceptingWork: boolean;
    running: boolean;
    batchSize: number;
  };
  tenantPaused: boolean;
};

export type WorkerRun = {
  workerId: string;
  recovered: number;
  claimed: number;
  completed: number;
  executed: boolean;
};

export type InboundMessage = {
  id: string;
  provider: "FAKE_INBOUND";
  externalEventId: string;
  externalMessageId: string | null;
  externalThreadId: string | null;
  channel: CampaignChannel;
  senderMasked: string;
  recipientMasked: string | null;
  receivedAt: string;
  payloadHash: string;
  status: "PENDING" | "PROCESSING" | "PROCESSED" | "QUARANTINED" | "DISCARDED" | "FAILED";
  associationStatus: "PENDING" | "ASSOCIATED" | "AMBIGUOUS" | "NOT_FOUND" | "DISCARDED";
  prospectId: string | null;
  contactId: string | null;
  activityId: string | null;
  quarantineReason: string | null;
  requeueCount: number;
  correlationId: string;
  createdAt: string;
  processedAt: string | null;
  discardedAt: string | null;
};

export type WebhookHealth = {
  provider: "FAKE_INBOUND";
  enabled: boolean;
  configured: boolean;
  maxPayloadBytes: number;
};
