CREATE TABLE opportunity (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  organization_id UUID NOT NULL REFERENCES organization (id),
  prospect_id UUID NOT NULL REFERENCES prospect (id),
  name TEXT NOT NULL,
  owner_user_id UUID NOT NULL REFERENCES app_user (id),
  stage TEXT NOT NULL,
  estimated_value NUMERIC(19, 2) NOT NULL DEFAULT 0,
  currency VARCHAR(3) NOT NULL,
  probability INTEGER NOT NULL,
  expected_close_date DATE,
  actual_close_date DATE,
  lost_reason TEXT,
  won_reason TEXT,
  source TEXT,
  primary_active BOOLEAN NOT NULL DEFAULT FALSE,
  stage_changed_at TIMESTAMPTZ NOT NULL,
  created_by UUID REFERENCES app_user (id),
  updated_by UUID REFERENCES app_user (id),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_opportunity_name CHECK (length(trim(name)) BETWEEN 1 AND 200),
  CONSTRAINT ck_opportunity_stage CHECK (
    stage IN ('QUALIFICATION', 'DISCOVERY', 'DEMO', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST')
  ),
  CONSTRAINT ck_opportunity_value CHECK (estimated_value >= 0),
  CONSTRAINT ck_opportunity_currency CHECK (currency ~ '^[A-Z]{3}$'),
  CONSTRAINT ck_opportunity_probability CHECK (probability BETWEEN 0 AND 100),
  CONSTRAINT ck_opportunity_close CHECK (
    (stage = 'WON' AND actual_close_date IS NOT NULL AND won_reason IS NOT NULL AND lost_reason IS NULL)
    OR (stage = 'LOST' AND actual_close_date IS NOT NULL AND lost_reason IS NOT NULL AND won_reason IS NULL)
    OR (stage NOT IN ('WON', 'LOST') AND actual_close_date IS NULL AND lost_reason IS NULL AND won_reason IS NULL)
  ),
  CONSTRAINT ck_opportunity_primary_active CHECK (
    NOT primary_active OR stage NOT IN ('WON', 'LOST')
  )
);

CREATE UNIQUE INDEX uk_opportunity_primary_active
  ON opportunity (organization_id, prospect_id)
  WHERE primary_active;
CREATE INDEX ix_opportunity_pipeline
  ON opportunity (organization_id, stage, owner_user_id, stage_changed_at, id);
CREATE INDEX ix_opportunity_prospect
  ON opportunity (organization_id, prospect_id, created_at DESC, id DESC);
CREATE INDEX ix_opportunity_expected_close
  ON opportunity (organization_id, expected_close_date)
  WHERE stage NOT IN ('WON', 'LOST');

CREATE TABLE opportunity_stage_history (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  opportunity_id UUID NOT NULL REFERENCES opportunity (id),
  actor_user_id UUID REFERENCES app_user (id),
  previous_stage TEXT,
  new_stage TEXT NOT NULL,
  reason TEXT,
  comment TEXT,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_opportunity_history
  ON opportunity_stage_history (organization_id, opportunity_id, created_at DESC, id DESC);
