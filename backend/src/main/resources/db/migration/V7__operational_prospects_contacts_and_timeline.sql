ALTER TABLE institution
  ADD COLUMN legal_name TEXT,
  ADD COLUMN address TEXT,
  ADD COLUMN timezone TEXT,
  ADD COLUMN archived_at TIMESTAMPTZ;

ALTER TABLE prospect
  ADD COLUMN eligibility TEXT NOT NULL DEFAULT 'ELIGIBLE',
  ADD COLUMN source_detail TEXT,
  ADD COLUMN owner_user_id UUID REFERENCES app_user (id),
  ADD COLUMN notes_summary TEXT,
  ADD COLUMN created_by UUID REFERENCES app_user (id),
  ADD COLUMN updated_by UUID REFERENCES app_user (id),
  ADD COLUMN archived_at TIMESTAMPTZ,
  ADD COLUMN lost_reason TEXT,
  ADD COLUMN status_detail_at TIMESTAMPTZ;

UPDATE prospect
SET eligibility = CASE WHEN contact_eligible THEN 'ELIGIBLE' ELSE 'EXCLUDED' END;

UPDATE prospect SET status = CASE status
  WHEN 'NEEDS_ENRICHMENT' THEN 'QUALIFYING'
  WHEN 'READY_FOR_REVIEW' THEN 'QUALIFYING'
  WHEN 'APPROVED' THEN 'READY_TO_CONTACT'
  WHEN 'QUEUED' THEN 'READY_TO_CONTACT'
  WHEN 'QUALIFIED' THEN 'INTERESTED'
  WHEN 'TRIAL_PROPOSED' THEN 'DEMO_PROPOSED'
  WHEN 'TRIAL_ACTIVE' THEN 'DEMO_SCHEDULED'
  WHEN 'QUOTED' THEN 'PROPOSAL'
  WHEN 'WON' THEN 'CUSTOMER'
  WHEN 'NO_RESPONSE' THEN 'FOLLOW_UP'
  WHEN 'BOUNCED' THEN 'DO_NOT_CONTACT'
  WHEN 'UNSUBSCRIBED' THEN 'DO_NOT_CONTACT'
  ELSE status
END;

CREATE INDEX ix_prospect_org_owner ON prospect (organization_id, owner_user_id);
CREATE INDEX ix_prospect_org_next_action ON prospect (organization_id, next_action_at)
  WHERE archived_at IS NULL;
CREATE INDEX ix_prospect_org_search ON prospect (organization_id, status, priority, score);

ALTER TABLE contact
  ADD COLUMN first_name TEXT,
  ADD COLUMN last_name TEXT,
  ADD COLUMN primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN preferred_channel TEXT,
  ADD COLUMN consent TEXT NOT NULL DEFAULT 'UNKNOWN',
  ADD COLUMN source TEXT,
  ADD COLUMN last_validated_at TIMESTAMPTZ,
  ADD COLUMN deleted_at TIMESTAMPTZ;

UPDATE contact SET first_name = name WHERE first_name IS NULL;

ALTER TABLE contact_channel
  ADD COLUMN valid BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN consent TEXT NOT NULL DEFAULT 'UNKNOWN',
  ADD COLUMN preferred BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN last_validated_at TIMESTAMPTZ;

CREATE UNIQUE INDEX uk_contact_primary_per_institution
  ON contact (organization_id, institution_id)
  WHERE primary_contact AND deleted_at IS NULL;

CREATE TABLE prospect_status_history (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  prospect_id UUID NOT NULL REFERENCES prospect (id),
  actor_user_id UUID REFERENCES app_user (id),
  previous_status TEXT NOT NULL,
  new_status TEXT NOT NULL,
  reason TEXT,
  comment TEXT,
  source TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_status_history_timeline
  ON prospect_status_history (organization_id, prospect_id, created_at DESC, id DESC);

CREATE TABLE prospect_note (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  prospect_id UUID NOT NULL REFERENCES prospect (id),
  author_user_id UUID REFERENCES app_user (id),
  body TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT ck_prospect_note_body CHECK (length(trim(body)) BETWEEN 1 AND 10000)
);

CREATE INDEX ix_prospect_note_timeline
  ON prospect_note (organization_id, prospect_id, created_at DESC, id DESC);

CREATE TABLE activity (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  prospect_id UUID NOT NULL REFERENCES prospect (id),
  contact_id UUID REFERENCES contact (id),
  actor_user_id UUID REFERENCES app_user (id),
  activity_type TEXT NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  channel TEXT,
  direction TEXT NOT NULL,
  outcome TEXT,
  summary TEXT NOT NULL,
  detail TEXT,
  external_reference TEXT,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_activity_direction CHECK (direction IN ('INBOUND', 'OUTBOUND', 'INTERNAL')),
  CONSTRAINT ck_activity_metadata_object CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX ix_activity_timeline
  ON activity (organization_id, prospect_id, occurred_at DESC, id DESC);
CREATE UNIQUE INDEX uk_activity_external_reference
  ON activity (organization_id, external_reference)
  WHERE external_reference IS NOT NULL;

CREATE TABLE crm_task (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  organization_id UUID NOT NULL REFERENCES organization (id),
  prospect_id UUID NOT NULL REFERENCES prospect (id),
  owner_user_id UUID NOT NULL REFERENCES app_user (id),
  creator_user_id UUID REFERENCES app_user (id),
  title TEXT NOT NULL,
  description TEXT,
  due_at TIMESTAMPTZ NOT NULL,
  priority TEXT NOT NULL,
  status TEXT NOT NULL,
  task_type TEXT NOT NULL,
  reminder_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  cancelled_at TIMESTAMPTZ,
  outcome TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_task_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
  CONSTRAINT ck_task_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
  CONSTRAINT ck_task_reminder CHECK (reminder_at IS NULL OR reminder_at <= due_at),
  CONSTRAINT ck_task_completion CHECK (
    (status = 'COMPLETED' AND completed_at IS NOT NULL AND cancelled_at IS NULL)
    OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND completed_at IS NULL)
    OR (status IN ('OPEN', 'IN_PROGRESS') AND completed_at IS NULL AND cancelled_at IS NULL)
  )
);

CREATE INDEX ix_task_owner_due
  ON crm_task (organization_id, owner_user_id, status, due_at);
CREATE INDEX ix_task_prospect_timeline
  ON crm_task (organization_id, prospect_id, created_at DESC, id DESC);
