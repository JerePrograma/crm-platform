ALTER TABLE campaign
  ADD COLUMN description TEXT,
  ADD COLUMN objective TEXT,
  ADD COLUMN channel TEXT NOT NULL DEFAULT 'EMAIL',
  ADD COLUMN owner_user_id UUID REFERENCES app_user (id),
  ADD COLUMN audience_filter JSONB NOT NULL DEFAULT '{}'::jsonb,
  ADD COLUMN template_version_id UUID REFERENCES template_version (id),
  ADD COLUMN scheduled_at TIMESTAMPTZ,
  ADD COLUMN approved_by UUID REFERENCES app_user (id),
  ADD COLUMN approved_at TIMESTAMPTZ,
  ADD COLUMN frozen_at TIMESTAMPTZ,
  ADD COLUMN simulated_at TIMESTAMPTZ,
  ADD COLUMN recipient_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN excluded_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN failure_message TEXT,
  ADD CONSTRAINT ck_campaign_channel CHECK (channel IN ('EMAIL', 'WHATSAPP')),
  ADD CONSTRAINT ck_campaign_audience_filter_object CHECK (jsonb_typeof(audience_filter) = 'object'),
  ADD CONSTRAINT ck_campaign_recipient_counts CHECK (recipient_count >= 0 AND excluded_count >= 0);

UPDATE campaign SET status = 'DRAFT'
WHERE status NOT IN (
  'DRAFT', 'READY_FOR_REVIEW', 'APPROVED', 'SIMULATED', 'SCHEDULED',
  'RUNNING', 'PAUSED', 'COMPLETED', 'CANCELLED', 'FAILED'
);

ALTER TABLE campaign
  ADD CONSTRAINT ck_campaign_status CHECK (status IN (
    'DRAFT', 'READY_FOR_REVIEW', 'APPROVED', 'SIMULATED', 'SCHEDULED',
    'RUNNING', 'PAUSED', 'COMPLETED', 'CANCELLED', 'FAILED'
  ));

ALTER TABLE campaign ALTER COLUMN channel DROP DEFAULT;

CREATE INDEX ix_campaign_org_status
  ON campaign (organization_id, status, updated_at DESC);

ALTER TABLE email_template
  ADD COLUMN channel TEXT NOT NULL DEFAULT 'EMAIL',
  ADD COLUMN archived_at TIMESTAMPTZ,
  ADD CONSTRAINT ck_email_template_channel CHECK (channel IN ('EMAIL', 'WHATSAPP'));

ALTER TABLE email_template ALTER COLUMN channel DROP DEFAULT;

ALTER TABLE template_version
  ADD COLUMN variables JSONB NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN archived_at TIMESTAMPTZ,
  ADD CONSTRAINT ck_template_variables_array CHECK (jsonb_typeof(variables) = 'array');

CREATE TABLE campaign_audience_recipient (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  campaign_id UUID NOT NULL REFERENCES campaign (id) ON DELETE CASCADE,
  prospect_id UUID NOT NULL REFERENCES prospect (id),
  contact_id UUID REFERENCES contact (id),
  contact_channel_id UUID REFERENCES contact_channel (id),
  included BOOLEAN NOT NULL,
  exclusion_reason TEXT,
  channel TEXT NOT NULL,
  validation_status TEXT NOT NULL,
  frozen_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_campaign_audience_prospect UNIQUE (organization_id, campaign_id, prospect_id),
  CONSTRAINT ck_audience_channel CHECK (channel IN ('EMAIL', 'WHATSAPP')),
  CONSTRAINT ck_audience_validation CHECK (validation_status IN ('VALID', 'MISSING_CHANNEL', 'EXCLUDED', 'INELIGIBLE')),
  CONSTRAINT ck_audience_reason CHECK (
    (included AND exclusion_reason IS NULL AND validation_status = 'VALID')
    OR (NOT included AND exclusion_reason IS NOT NULL AND validation_status <> 'VALID')
  )
);

CREATE INDEX ix_campaign_audience_execution
  ON campaign_audience_recipient (organization_id, campaign_id, included, prospect_id);

CREATE TABLE campaign_sequence_step (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  campaign_id UUID NOT NULL REFERENCES campaign (id) ON DELETE CASCADE,
  step_order INTEGER NOT NULL,
  step_type TEXT NOT NULL,
  configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_campaign_sequence_order UNIQUE (organization_id, campaign_id, step_order),
  CONSTRAINT ck_campaign_sequence_order CHECK (step_order > 0),
  CONSTRAINT ck_campaign_sequence_type CHECK (step_type IN ('EMAIL', 'WHATSAPP', 'MANUAL_TASK', 'WAIT', 'CONDITION', 'STOP')),
  CONSTRAINT ck_campaign_sequence_configuration CHECK (jsonb_typeof(configuration) = 'object')
);

CREATE TABLE campaign_simulation_run (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  campaign_id UUID NOT NULL REFERENCES campaign (id),
  idempotency_key TEXT NOT NULL,
  included_count INTEGER NOT NULL,
  excluded_count INTEGER NOT NULL,
  status TEXT NOT NULL,
  created_by UUID REFERENCES app_user (id),
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_campaign_simulation_idempotency UNIQUE (organization_id, idempotency_key),
  CONSTRAINT ck_campaign_simulation_counts CHECK (included_count >= 0 AND excluded_count >= 0),
  CONSTRAINT ck_campaign_simulation_status CHECK (status IN ('SIMULATED', 'FAILED'))
);

CREATE INDEX ix_campaign_simulation_campaign
  ON campaign_simulation_run (organization_id, campaign_id, created_at DESC);

CREATE TABLE campaign_simulation_result (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  simulation_run_id UUID NOT NULL REFERENCES campaign_simulation_run (id) ON DELETE CASCADE,
  campaign_id UUID NOT NULL REFERENCES campaign (id),
  prospect_id UUID NOT NULL REFERENCES prospect (id),
  contact_id UUID REFERENCES contact (id),
  result TEXT NOT NULL,
  reason TEXT,
  rendered_subject_hash VARCHAR(64),
  rendered_body_hash VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_campaign_simulation_result UNIQUE (organization_id, simulation_run_id, prospect_id),
  CONSTRAINT ck_campaign_simulation_result CHECK (result IN ('SIMULATED', 'EXCLUDED'))
);

CREATE INDEX ix_campaign_simulation_result_campaign
  ON campaign_simulation_result (organization_id, campaign_id, result);
