CREATE TABLE institution (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  name TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  category TEXT,
  locality TEXT,
  province TEXT,
  country TEXT,
  website TEXT,
  CONSTRAINT uk_institution_normalized_locality UNIQUE (normalized_name, locality)
);

CREATE TABLE contact (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  institution_id UUID NOT NULL REFERENCES institution (id),
  name TEXT,
  role TEXT
);

CREATE INDEX ix_contact_institution ON contact (institution_id);

CREATE TABLE contact_channel (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  contact_id UUID NOT NULL REFERENCES contact (id),
  type TEXT NOT NULL,
  value TEXT,
  normalized_value TEXT NOT NULL,
  primary_channel BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT uk_channel_type_normalized UNIQUE (type, normalized_value)
);

CREATE INDEX ix_contact_channel_contact ON contact_channel (contact_id);
CREATE INDEX ix_contact_channel_normalized ON contact_channel (normalized_value);

CREATE TABLE prospect (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  institution_id UUID NOT NULL REFERENCES institution (id),
  external_source_id TEXT UNIQUE,
  status TEXT NOT NULL,
  priority INTEGER,
  score INTEGER,
  estimated_students INTEGER,
  current_tools TEXT,
  administrative_pain TEXT,
  source TEXT,
  evidence TEXT,
  verified_at TIMESTAMPTZ,
  last_contact_at TIMESTAMPTZ,
  next_action_at TIMESTAMPTZ,
  owner TEXT,
  contact_eligible BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX ix_prospect_institution ON prospect (institution_id);
CREATE INDEX ix_prospect_status ON prospect (status);
CREATE INDEX ix_prospect_score ON prospect (score);

CREATE TABLE exclusion (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  channel_type TEXT NOT NULL,
  normalized_value TEXT NOT NULL,
  reason TEXT NOT NULL,
  CONSTRAINT uk_exclusion_channel UNIQUE (channel_type, normalized_value)
);

CREATE INDEX ix_exclusion_normalized_value ON exclusion (normalized_value);

CREATE TABLE campaign (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  name TEXT NOT NULL,
  status TEXT NOT NULL,
  dry_run BOOLEAN NOT NULL DEFAULT TRUE,
  daily_limit INTEGER NOT NULL DEFAULT 0,
  approved BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT ck_campaign_daily_limit_non_negative CHECK (daily_limit >= 0)
);

CREATE TABLE email_template (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE template_version (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  template_id UUID NOT NULL REFERENCES email_template (id),
  version_number INTEGER NOT NULL,
  subject TEXT NOT NULL,
  html_body TEXT NOT NULL,
  text_body TEXT NOT NULL,
  CONSTRAINT uk_template_version UNIQUE (template_id, version_number),
  CONSTRAINT ck_template_version_positive CHECK (version_number > 0)
);

CREATE TABLE attachment (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  file_name TEXT NOT NULL,
  mime_type TEXT NOT NULL,
  size_bytes BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  storage_uri TEXT NOT NULL,
  CONSTRAINT ck_attachment_size_non_negative CHECK (size_bytes >= 0),
  CONSTRAINT ck_attachment_sha256_length CHECK (length(sha256) = 64)
);

CREATE TABLE audit_event (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  action TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id TEXT,
  payload JSONB
);

CREATE INDEX ix_audit_entity ON audit_event (entity_type, entity_id);
CREATE INDEX ix_audit_created_at ON audit_event (created_at);

CREATE TABLE system_setting (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  setting_key TEXT NOT NULL UNIQUE,
  setting_value TEXT NOT NULL
);

INSERT INTO system_setting (
  id,
  version,
  created_at,
  updated_at,
  setting_key,
  setting_value
)
VALUES
  ('00000000-0000-0000-0000-000000000001', 0, now(), now(), 'sending.kill-switch', 'true'),
  ('00000000-0000-0000-0000-000000000002', 0, now(), now(), 'sending.enabled', 'false'),
  ('00000000-0000-0000-0000-000000000003', 0, now(), now(), 'sending.dry-run', 'true'),
  ('00000000-0000-0000-0000-000000000004', 0, now(), now(), 'sending.daily-limit', '0');
