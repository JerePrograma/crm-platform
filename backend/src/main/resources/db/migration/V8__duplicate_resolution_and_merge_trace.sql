ALTER TABLE prospect
  ADD COLUMN merged_into_id UUID REFERENCES prospect (id),
  ADD COLUMN merged_at TIMESTAMPTZ,
  ADD COLUMN merged_by UUID REFERENCES app_user (id),
  ADD CONSTRAINT ck_prospect_not_merged_into_self CHECK (merged_into_id IS NULL OR merged_into_id <> id),
  ADD CONSTRAINT ck_prospect_merge_state CHECK (
    (merged_into_id IS NULL AND merged_at IS NULL)
    OR (merged_into_id IS NOT NULL AND merged_at IS NOT NULL AND status = 'DUPLICATE')
  );

CREATE INDEX ix_prospect_merged_into
  ON prospect (organization_id, merged_into_id)
  WHERE merged_into_id IS NOT NULL;

ALTER TABLE duplicate_review
  ADD COLUMN resolution_action TEXT,
  ADD COLUMN resolved_at TIMESTAMPTZ,
  ADD COLUMN resolved_by UUID REFERENCES app_user (id),
  ADD COLUMN survivor_prospect_id UUID REFERENCES prospect (id),
  ADD COLUMN absorbed_prospect_id UUID REFERENCES prospect (id),
  ADD COLUMN resolution_key TEXT,
  ADD COLUMN resolution_comment TEXT;

CREATE UNIQUE INDEX uk_duplicate_review_resolution_key
  ON duplicate_review (organization_id, resolution_key)
  WHERE resolution_key IS NOT NULL;

CREATE INDEX ix_duplicate_review_queue
  ON duplicate_review (organization_id, status, created_at, id);

CREATE TABLE prospect_merge_map (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  survivor_prospect_id UUID NOT NULL REFERENCES prospect (id),
  absorbed_prospect_id UUID NOT NULL REFERENCES prospect (id),
  duplicate_review_id UUID REFERENCES duplicate_review (id),
  merged_by UUID REFERENCES app_user (id),
  idempotency_key TEXT NOT NULL,
  merged_at TIMESTAMPTZ NOT NULL,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT ck_prospect_merge_distinct CHECK (survivor_prospect_id <> absorbed_prospect_id),
  CONSTRAINT ck_prospect_merge_metadata_object CHECK (jsonb_typeof(metadata) = 'object'),
  CONSTRAINT uk_prospect_merge_absorbed UNIQUE (organization_id, absorbed_prospect_id),
  CONSTRAINT uk_prospect_merge_key UNIQUE (organization_id, idempotency_key)
);

CREATE INDEX ix_prospect_merge_survivor
  ON prospect_merge_map (organization_id, survivor_prospect_id, merged_at DESC);

CREATE TABLE contact_merge_map (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  survivor_contact_id UUID NOT NULL REFERENCES contact (id),
  absorbed_contact_id UUID NOT NULL REFERENCES contact (id),
  prospect_merge_id UUID NOT NULL REFERENCES prospect_merge_map (id),
  merged_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_contact_merge_distinct CHECK (survivor_contact_id <> absorbed_contact_id),
  CONSTRAINT uk_contact_merge_absorbed UNIQUE (organization_id, absorbed_contact_id)
);
