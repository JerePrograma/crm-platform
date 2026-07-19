ALTER TABLE import_row
  ADD COLUMN source_sheet TEXT NOT NULL DEFAULT 'Prospectos';

ALTER TABLE import_row
  DROP CONSTRAINT uk_import_row_number;

ALTER TABLE import_row
  ADD CONSTRAINT uk_import_row_source_number
    UNIQUE (import_job_id, source_sheet, row_number);

CREATE INDEX ix_import_row_source ON import_row (import_job_id, source_sheet);
