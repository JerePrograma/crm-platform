export type ProspectStatus =
  | "NEW"
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

export type Prospect = {
  id: string;
  version: number;
  institutionId: string;
  institutionName: string;
  category: string | null;
  locality: string | null;
  province: string | null;
  country: string | null;
  website: string | null;
  status: ProspectStatus;
  priority: number | null;
  score: number | null;
  estimatedStudents: number | null;
  source: string | null;
  owner: string | null;
  contactEligible: boolean;
  createdAt: string;
  updatedAt: string;
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
  importRowId: string;
  sourceSheet: string;
  rowNumber: number;
  matchType: string;
  confidence: number;
  existingProspectId: string | null;
  status: string;
  notes: string | null;
  createdAt: string;
};

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

export type Credentials = {
  username: string;
  password: string;
};
