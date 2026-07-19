ALTER TABLE institution
  ADD COLUMN normalized_locality TEXT,
  ADD COLUMN website_domain TEXT;

UPDATE institution
SET normalized_locality = lower(trim(locality))
WHERE locality IS NOT NULL;

ALTER TABLE institution
  DROP CONSTRAINT uk_institution_normalized_locality;

ALTER TABLE institution
  ADD CONSTRAINT uk_institution_normalized_location
    UNIQUE (normalized_name, normalized_locality);

CREATE INDEX ix_institution_website_domain ON institution (website_domain);
