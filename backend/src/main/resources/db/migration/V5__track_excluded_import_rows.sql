ALTER TABLE import_job
  ADD COLUMN excluded_rows INTEGER NOT NULL DEFAULT 0;

ALTER TABLE import_job
  DROP CONSTRAINT ck_import_job_counts_non_negative;

ALTER TABLE import_job
  ADD CONSTRAINT ck_import_job_counts_non_negative CHECK (
    total_rows >= 0
    AND accepted_rows >= 0
    AND excluded_rows >= 0
    AND rejected_rows >= 0
    AND duplicate_rows >= 0
    AND review_rows >= 0
  );
