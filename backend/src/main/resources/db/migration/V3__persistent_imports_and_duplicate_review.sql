ALTER TABLE prospect
  ADD CONSTRAINT uk_prospect_institution UNIQUE (institution_id);

CREATE TABLE import_job (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  file_name TEXT NOT NULL,
  file_sha256 VARCHAR(64) NOT NULL,
  idempotency_key TEXT NOT NULL UNIQUE,
  source_type TEXT NOT NULL,
  dry_run BOOLEAN NOT NULL,
  status TEXT NOT NULL,
  total_rows INTEGER NOT NULL DEFAULT 0,
  accepted_rows INTEGER NOT NULL DEFAULT 0,
  rejected_rows INTEGER NOT NULL DEFAULT 0,
  duplicate_rows INTEGER NOT NULL DEFAULT 0,
  review_rows INTEGER NOT NULL DEFAULT 0,
  error_message TEXT,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  CONSTRAINT ck_import_job_sha256_length CHECK (length(file_sha256) = 64),
  CONSTRAINT ck_import_job_counts_non_negative CHECK (
    total_rows >= 0
    AND accepted_rows >= 0
    AND rejected_rows >= 0
    AND duplicate_rows >= 0
    AND review_rows >= 0
  )
);

CREATE INDEX ix_import_job_status ON import_job (status);
CREATE INDEX ix_import_job_created_at ON import_job (created_at);

CREATE TABLE import_row (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  import_job_id UUID NOT NULL REFERENCES import_job (id) ON DELETE CASCADE,
  row_number INTEGER NOT NULL,
  raw_data TEXT NOT NULL,
  normalized_email TEXT,
  normalized_phone TEXT,
  status TEXT NOT NULL,
  error_message TEXT,
  prospect_id UUID REFERENCES prospect (id),
  CONSTRAINT uk_import_row_number UNIQUE (import_job_id, row_number),
  CONSTRAINT ck_import_row_number_positive CHECK (row_number > 0)
);

CREATE INDEX ix_import_row_job_status ON import_row (import_job_id, status);
CREATE INDEX ix_import_row_email ON import_row (normalized_email);
CREATE INDEX ix_import_row_phone ON import_row (normalized_phone);

CREATE TABLE duplicate_review (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  import_row_id UUID NOT NULL REFERENCES import_row (id) ON DELETE CASCADE,
  existing_prospect_id UUID REFERENCES prospect (id),
  match_type TEXT NOT NULL,
  confidence NUMERIC(5, 4) NOT NULL,
  status TEXT NOT NULL,
  notes TEXT,
  CONSTRAINT uk_duplicate_review_row UNIQUE (import_row_id),
  CONSTRAINT ck_duplicate_confidence CHECK (confidence >= 0 AND confidence <= 1)
);

CREATE INDEX ix_duplicate_review_status ON duplicate_review (status);
