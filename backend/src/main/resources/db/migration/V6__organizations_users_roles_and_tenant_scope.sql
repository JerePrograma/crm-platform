CREATE TABLE organization (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  slug TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  timezone TEXT NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
  currency CHAR(3) NOT NULL DEFAULT 'ARS',
  locale TEXT NOT NULL DEFAULT 'es-AR',
  active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO organization (id, created_at, updated_at, slug, name)
VALUES (
  '00000000-0000-0000-0000-000000000010',
  now(),
  now(),
  'gestudio-local',
  'Gestudio Local'
);

CREATE TABLE app_user (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  username TEXT NOT NULL,
  normalized_username TEXT NOT NULL UNIQUE,
  display_name TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  failed_attempts INTEGER NOT NULL DEFAULT 0,
  locked_until TIMESTAMPTZ,
  password_changed_at TIMESTAMPTZ NOT NULL,
  last_login_at TIMESTAMPTZ,
  CONSTRAINT ck_app_user_failed_attempts CHECK (failed_attempts >= 0)
);

CREATE TABLE permission (
  code TEXT PRIMARY KEY,
  description TEXT NOT NULL
);

INSERT INTO permission (code, description) VALUES
  ('USER_MANAGE', 'Manage users and memberships'),
  ('SETTINGS_MANAGE', 'Manage organization settings'),
  ('PROSPECT_READ', 'Read prospects and contacts'),
  ('PROSPECT_WRITE', 'Create and edit prospects and contacts'),
  ('PROSPECT_ASSIGN', 'Assign prospect ownership'),
  ('IMPORT_PREVIEW', 'Preview imports'),
  ('IMPORT_EXECUTE', 'Execute imports'),
  ('DUPLICATE_RESOLVE', 'Resolve duplicate reviews'),
  ('ACTIVITY_WRITE', 'Write notes, activities and tasks'),
  ('OPPORTUNITY_WRITE', 'Manage opportunities'),
  ('CAMPAIGN_READ', 'Read campaigns and templates'),
  ('CAMPAIGN_WRITE', 'Create and edit campaigns'),
  ('CAMPAIGN_APPROVE', 'Approve campaigns'),
  ('MESSAGE_DRAFT', 'Create message drafts'),
  ('MESSAGE_SIMULATE', 'Simulate message dispatch'),
  ('MESSAGE_SEND', 'Request real message dispatch'),
  ('AUDIT_READ', 'Read audit trail'),
  ('REPORT_READ', 'Read and export reports');

CREATE TABLE crm_role (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  name TEXT NOT NULL,
  description TEXT NOT NULL,
  CONSTRAINT uk_crm_role_org_name UNIQUE (organization_id, name)
);

INSERT INTO crm_role (id, organization_id, name, description) VALUES
  ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000010', 'ADMIN', 'Full organization administration'),
  ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000010', 'MANAGER', 'Commercial management and approval'),
  ('00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000010', 'SALES', 'Daily commercial operation'),
  ('00000000-0000-0000-0000-000000000104', '00000000-0000-0000-0000-000000000010', 'VIEWER', 'Read-only commercial access');

CREATE TABLE role_permission (
  role_id UUID NOT NULL REFERENCES crm_role (id) ON DELETE CASCADE,
  permission_code TEXT NOT NULL REFERENCES permission (code),
  PRIMARY KEY (role_id, permission_code)
);

INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000101', code FROM permission;

INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000102', code
FROM permission
WHERE code NOT IN ('USER_MANAGE', 'SETTINGS_MANAGE', 'MESSAGE_SEND');

INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000103', code
FROM permission
WHERE code IN (
  'PROSPECT_READ', 'PROSPECT_WRITE', 'PROSPECT_ASSIGN', 'IMPORT_PREVIEW',
  'IMPORT_EXECUTE', 'DUPLICATE_RESOLVE', 'ACTIVITY_WRITE', 'OPPORTUNITY_WRITE',
  'CAMPAIGN_READ', 'CAMPAIGN_WRITE', 'MESSAGE_DRAFT', 'MESSAGE_SIMULATE',
  'REPORT_READ'
);

INSERT INTO role_permission (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000104', code
FROM permission
WHERE code IN ('PROSPECT_READ', 'CAMPAIGN_READ', 'REPORT_READ');

CREATE TABLE organization_membership (
  user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  organization_id UUID NOT NULL REFERENCES organization (id),
  role_id UUID NOT NULL REFERENCES crm_role (id),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (user_id, organization_id)
);

CREATE INDEX ix_membership_organization ON organization_membership (organization_id, active);
CREATE INDEX ix_app_user_active ON app_user (active, normalized_username);

DO $$
DECLARE
  table_name TEXT;
BEGIN
  FOREACH table_name IN ARRAY ARRAY[
    'institution', 'contact', 'contact_channel', 'prospect', 'exclusion',
    'campaign', 'email_template', 'template_version', 'attachment', 'audit_event',
    'system_setting', 'import_job', 'import_row', 'duplicate_review'
  ]
  LOOP
    EXECUTE format(
      'ALTER TABLE %I ADD COLUMN organization_id UUID NOT NULL DEFAULT %L REFERENCES organization (id)',
      table_name,
      '00000000-0000-0000-0000-000000000010'
    );
    EXECUTE format('ALTER TABLE %I ALTER COLUMN organization_id DROP DEFAULT', table_name);
    EXECUTE format('CREATE INDEX %I ON %I (organization_id)', 'ix_' || table_name || '_org', table_name);
  END LOOP;
END $$;

ALTER TABLE institution DROP CONSTRAINT uk_institution_normalized_location;
ALTER TABLE institution ADD CONSTRAINT uk_institution_org_normalized_location
  UNIQUE (organization_id, normalized_name, normalized_locality);

ALTER TABLE contact_channel DROP CONSTRAINT uk_channel_type_normalized;
ALTER TABLE contact_channel ADD CONSTRAINT uk_channel_org_type_normalized
  UNIQUE (organization_id, type, normalized_value);

ALTER TABLE prospect DROP CONSTRAINT prospect_external_source_id_key;
ALTER TABLE prospect ADD CONSTRAINT uk_prospect_org_external_source
  UNIQUE (organization_id, external_source_id);
ALTER TABLE prospect DROP CONSTRAINT uk_prospect_institution;
ALTER TABLE prospect ADD CONSTRAINT uk_prospect_org_institution
  UNIQUE (organization_id, institution_id);

ALTER TABLE exclusion DROP CONSTRAINT uk_exclusion_channel;
ALTER TABLE exclusion ADD CONSTRAINT uk_exclusion_org_channel
  UNIQUE (organization_id, channel_type, normalized_value);

ALTER TABLE email_template DROP CONSTRAINT email_template_name_key;
ALTER TABLE email_template ADD CONSTRAINT uk_email_template_org_name
  UNIQUE (organization_id, name);

ALTER TABLE template_version DROP CONSTRAINT uk_template_version;
ALTER TABLE template_version ADD CONSTRAINT uk_template_version_org
  UNIQUE (organization_id, template_id, version_number);

ALTER TABLE system_setting DROP CONSTRAINT system_setting_setting_key_key;
ALTER TABLE system_setting ADD CONSTRAINT uk_system_setting_org_key
  UNIQUE (organization_id, setting_key);

ALTER TABLE import_job DROP CONSTRAINT import_job_idempotency_key_key;
ALTER TABLE import_job ADD CONSTRAINT uk_import_job_org_idempotency
  UNIQUE (organization_id, idempotency_key);

ALTER TABLE import_row DROP CONSTRAINT uk_import_row_source_number;
ALTER TABLE import_row ADD CONSTRAINT uk_import_row_org_source_number
  UNIQUE (organization_id, import_job_id, source_sheet, row_number);

ALTER TABLE audit_event
  ADD COLUMN actor_user_id UUID REFERENCES app_user (id),
  ADD COLUMN result TEXT NOT NULL DEFAULT 'SUCCESS',
  ADD COLUMN source TEXT NOT NULL DEFAULT 'APPLICATION',
  ADD COLUMN correlation_id TEXT;

CREATE INDEX ix_audit_org_created ON audit_event (organization_id, created_at DESC);
CREATE INDEX ix_audit_actor ON audit_event (organization_id, actor_user_id, created_at DESC);
