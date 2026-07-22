CREATE TABLE outbox_event (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  event_type TEXT NOT NULL,
  event_version INTEGER NOT NULL,
  aggregate_type TEXT NOT NULL,
  aggregate_id UUID NOT NULL,
  payload JSONB NOT NULL,
  request_hash CHAR(64) NOT NULL,
  status TEXT NOT NULL,
  attempt_count INTEGER NOT NULL DEFAULT 0,
  max_attempts INTEGER NOT NULL DEFAULT 5,
  next_attempt_at TIMESTAMPTZ NOT NULL,
  locked_at TIMESTAMPTZ,
  lock_expires_at TIMESTAMPTZ,
  locked_by TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  processed_at TIMESTAMPTZ,
  last_error_code TEXT,
  last_error_summary TEXT,
  result_summary TEXT,
  idempotency_key TEXT NOT NULL,
  correlation_id TEXT NOT NULL,
  created_by UUID REFERENCES app_user (id),
  CONSTRAINT uk_outbox_org_idempotency UNIQUE (organization_id, idempotency_key),
  CONSTRAINT ck_outbox_event_type CHECK (event_type ~ '^[A-Z][A-Z0-9_]{2,99}$'),
  CONSTRAINT ck_outbox_event_version CHECK (event_version > 0),
  CONSTRAINT ck_outbox_aggregate CHECK (
    aggregate_type ~ '^[A-Z][A-Z0-9_]{1,49}$'
  ),
  CONSTRAINT ck_outbox_payload_object CHECK (jsonb_typeof(payload) = 'object'),
  CONSTRAINT ck_outbox_payload_size CHECK (octet_length(payload::text) <= 65536),
  CONSTRAINT ck_outbox_request_hash CHECK (request_hash ~ '^[0-9a-f]{64}$'),
  CONSTRAINT ck_outbox_status CHECK (status IN (
    'PENDING', 'PROCESSING', 'SUCCEEDED', 'RETRY', 'DEAD', 'CANCELLED', 'BLOCKED'
  )),
  CONSTRAINT ck_outbox_attempts CHECK (
    max_attempts BETWEEN 1 AND 20 AND attempt_count BETWEEN 0 AND max_attempts
  ),
  CONSTRAINT ck_outbox_lock_pair CHECK (
    (locked_at IS NULL AND lock_expires_at IS NULL AND locked_by IS NULL)
    OR (locked_at IS NOT NULL AND lock_expires_at IS NOT NULL AND locked_by IS NOT NULL
      AND lock_expires_at > locked_at)
  ),
  CONSTRAINT ck_outbox_processing_lock CHECK (
    status <> 'PROCESSING'
    OR (locked_at IS NOT NULL AND lock_expires_at IS NOT NULL AND locked_by IS NOT NULL)
  ),
  CONSTRAINT ck_outbox_processed_at CHECK (
    (status IN ('SUCCEEDED', 'DEAD', 'CANCELLED', 'BLOCKED') AND processed_at IS NOT NULL)
    OR (status IN ('PENDING', 'PROCESSING', 'RETRY') AND processed_at IS NULL)
  ),
  CONSTRAINT ck_outbox_correlation CHECK (length(correlation_id) BETWEEN 1 AND 128),
  CONSTRAINT ck_outbox_idempotency CHECK (length(idempotency_key) BETWEEN 1 AND 200)
);

CREATE INDEX ix_outbox_poll
  ON outbox_event (status, next_attempt_at, created_at, id)
  WHERE status IN ('PENDING', 'RETRY');
CREATE INDEX ix_outbox_org_status_next
  ON outbox_event (organization_id, status, next_attempt_at, created_at DESC);
CREATE INDEX ix_outbox_aggregate
  ON outbox_event (organization_id, aggregate_type, aggregate_id, created_at DESC);
CREATE INDEX ix_outbox_correlation
  ON outbox_event (organization_id, correlation_id, created_at DESC);
CREATE INDEX ix_outbox_expired_lease
  ON outbox_event (lock_expires_at)
  WHERE status = 'PROCESSING';

CREATE TABLE inbound_message (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  provider TEXT NOT NULL,
  external_event_id TEXT NOT NULL,
  external_message_id TEXT,
  external_thread_id TEXT,
  channel TEXT NOT NULL,
  sender_normalized TEXT NOT NULL,
  recipient_normalized TEXT,
  received_at TIMESTAMPTZ NOT NULL,
  payload_hash CHAR(64) NOT NULL,
  body_excerpt TEXT,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  status TEXT NOT NULL,
  association_status TEXT NOT NULL,
  prospect_id UUID REFERENCES prospect (id),
  contact_id UUID REFERENCES contact (id),
  activity_id UUID REFERENCES activity (id),
  quarantine_reason TEXT,
  requeue_count INTEGER NOT NULL DEFAULT 0,
  correlation_id TEXT NOT NULL,
  nonce_hash CHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  processed_at TIMESTAMPTZ,
  discarded_at TIMESTAMPTZ,
  CONSTRAINT uk_inbound_org_provider_event
    UNIQUE (organization_id, provider, external_event_id),
  CONSTRAINT uk_inbound_org_provider_nonce
    UNIQUE (organization_id, provider, nonce_hash),
  CONSTRAINT ck_inbound_provider CHECK (provider IN ('FAKE_INBOUND')),
  CONSTRAINT ck_inbound_channel CHECK (channel IN ('EMAIL', 'WHATSAPP')),
  CONSTRAINT ck_inbound_payload_hash CHECK (payload_hash ~ '^[0-9a-f]{64}$'),
  CONSTRAINT ck_inbound_nonce_hash CHECK (nonce_hash ~ '^[0-9a-f]{64}$'),
  CONSTRAINT ck_inbound_metadata_object CHECK (jsonb_typeof(metadata) = 'object'),
  CONSTRAINT ck_inbound_metadata_size CHECK (octet_length(metadata::text) <= 8192),
  CONSTRAINT ck_inbound_excerpt_size CHECK (body_excerpt IS NULL OR length(body_excerpt) <= 500),
  CONSTRAINT ck_inbound_requeue_count CHECK (requeue_count BETWEEN 0 AND 100),
  CONSTRAINT ck_inbound_status CHECK (
    status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'QUARANTINED', 'DISCARDED', 'FAILED')
  ),
  CONSTRAINT ck_inbound_association CHECK (
    association_status IN ('PENDING', 'ASSOCIATED', 'AMBIGUOUS', 'NOT_FOUND', 'DISCARDED')
  ),
  CONSTRAINT ck_inbound_processed CHECK (
    (status IN ('PROCESSED', 'QUARANTINED', 'DISCARDED', 'FAILED') AND processed_at IS NOT NULL)
    OR (status IN ('PENDING', 'PROCESSING') AND processed_at IS NULL)
  ),
  CONSTRAINT ck_inbound_discarded CHECK (
    (status = 'DISCARDED' AND discarded_at IS NOT NULL AND quarantine_reason IS NOT NULL)
    OR (status <> 'DISCARDED' AND discarded_at IS NULL)
  ),
  CONSTRAINT ck_inbound_correlation CHECK (length(correlation_id) BETWEEN 1 AND 128)
);

CREATE INDEX ix_inbound_association
  ON inbound_message (organization_id, channel, sender_normalized, received_at DESC);
CREATE INDEX ix_inbound_external_thread
  ON inbound_message (organization_id, provider, external_thread_id)
  WHERE external_thread_id IS NOT NULL;
CREATE INDEX ix_inbound_external_message
  ON inbound_message (organization_id, provider, external_message_id)
  WHERE external_message_id IS NOT NULL;
CREATE INDEX ix_inbound_quarantine
  ON inbound_message (organization_id, status, created_at DESC)
  WHERE status = 'QUARANTINED';
CREATE INDEX ix_inbound_prospect
  ON inbound_message (organization_id, prospect_id, received_at DESC)
  WHERE prospect_id IS NOT NULL;

ALTER TABLE message_record
  ADD COLUMN request_hash CHAR(64);

UPDATE message_record
SET request_hash = encode(sha256(convert_to(idempotency_key, 'UTF8')), 'hex')
WHERE idempotency_key IS NOT NULL;

ALTER TABLE message_record
  ADD CONSTRAINT ck_message_request_hash CHECK (
    request_hash IS NULL OR request_hash ~ '^[0-9a-f]{64}$'
  );
