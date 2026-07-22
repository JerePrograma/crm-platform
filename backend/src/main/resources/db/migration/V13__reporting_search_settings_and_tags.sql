CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE organization
  ADD COLUMN branding_primary_color TEXT NOT NULL DEFAULT '#0f766e',
  ADD COLUMN follow_up_days INTEGER NOT NULL DEFAULT 3,
  ADD COLUMN operating_window_start TIME NOT NULL DEFAULT TIME '09:00',
  ADD COLUMN operating_window_end TIME NOT NULL DEFAULT TIME '18:00',
  ADD COLUMN business_days SMALLINT[] NOT NULL DEFAULT ARRAY[1, 2, 3, 4, 5]::SMALLINT[],
  ADD COLUMN campaign_daily_limit INTEGER NOT NULL DEFAULT 0,
  ADD CONSTRAINT ck_organization_branding_color
    CHECK (branding_primary_color ~ '^#[0-9A-Fa-f]{6}$'),
  ADD CONSTRAINT ck_organization_follow_up_days CHECK (follow_up_days BETWEEN 1 AND 365),
  ADD CONSTRAINT ck_organization_operating_window
    CHECK (operating_window_start < operating_window_end),
  ADD CONSTRAINT ck_organization_business_days
    CHECK (business_days <@ ARRAY[1, 2, 3, 4, 5, 6, 7]::SMALLINT[]
      AND cardinality(business_days) BETWEEN 1 AND 7),
  ADD CONSTRAINT ck_organization_campaign_daily_limit
    CHECK (campaign_daily_limit >= 0);

CREATE TABLE crm_tag (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  organization_id UUID NOT NULL REFERENCES organization (id),
  name TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  color TEXT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_by UUID REFERENCES app_user (id),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_crm_tag_org_name UNIQUE (organization_id, normalized_name),
  CONSTRAINT ck_crm_tag_name CHECK (char_length(name) BETWEEN 1 AND 80),
  CONSTRAINT ck_crm_tag_color CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE TABLE prospect_tag (
  organization_id UUID NOT NULL REFERENCES organization (id),
  prospect_id UUID NOT NULL REFERENCES prospect (id),
  tag_id UUID NOT NULL REFERENCES crm_tag (id),
  assigned_by UUID REFERENCES app_user (id),
  assigned_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (organization_id, prospect_id, tag_id)
);

CREATE INDEX ix_crm_tag_org_active ON crm_tag (organization_id, active, normalized_name);
CREATE INDEX ix_prospect_tag_tag ON prospect_tag (organization_id, tag_id, prospect_id);
CREATE INDEX ix_prospect_tag_prospect ON prospect_tag (organization_id, prospect_id, tag_id);

CREATE INDEX ix_institution_name_trgm
  ON institution USING gin (lower(name) gin_trgm_ops);
CREATE INDEX ix_institution_legal_name_trgm
  ON institution USING gin (lower(coalesce(legal_name, '')) gin_trgm_ops);
CREATE INDEX ix_institution_location_trgm
  ON institution USING gin (lower(coalesce(locality, '') || ' ' || coalesce(province, '')) gin_trgm_ops);
CREATE INDEX ix_institution_website_trgm
  ON institution USING gin (lower(coalesce(website, '')) gin_trgm_ops);
CREATE INDEX ix_contact_name_trgm
  ON contact USING gin (lower(coalesce(name, '')) gin_trgm_ops)
  WHERE deleted_at IS NULL;
CREATE INDEX ix_contact_channel_value_trgm
  ON contact_channel USING gin (lower(normalized_value) gin_trgm_ops);
CREATE INDEX ix_prospect_notes_trgm
  ON prospect USING gin (lower(coalesce(notes_summary, '')) gin_trgm_ops)
  WHERE archived_at IS NULL;

CREATE INDEX ix_prospect_org_source ON prospect (organization_id, source)
  WHERE archived_at IS NULL;
CREATE INDEX ix_prospect_org_owner_status ON prospect (organization_id, owner_user_id, status)
  WHERE archived_at IS NULL;
CREATE INDEX ix_activity_org_occurred_channel
  ON activity (organization_id, occurred_at DESC, channel);
CREATE INDEX ix_task_org_due_status ON crm_task (organization_id, due_at, status);
CREATE INDEX ix_opportunity_org_stage_changed
  ON opportunity (organization_id, stage, stage_changed_at);

COMMENT ON TABLE crm_tag IS 'Tenant-scoped accessible labels; historical assignments are preserved by deactivation.';
COMMENT ON COLUMN organization.campaign_daily_limit IS
  'Organization preference only. Environment and persistent sending guards remain dominant.';
