CREATE TABLE integration_connection (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  organization_id UUID NOT NULL REFERENCES organization (id),
  provider TEXT NOT NULL,
  mode TEXT NOT NULL,
  status TEXT NOT NULL,
  configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
  encrypted_credential BYTEA,
  credential_key_id TEXT,
  cursor_value TEXT,
  connected_at TIMESTAMPTZ,
  disconnected_at TIMESTAMPTZ,
  last_checked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_integration_connection_org_provider UNIQUE (organization_id, provider),
  CONSTRAINT ck_integration_provider CHECK (provider IN ('GMAIL', 'WHATSAPP_CLOUD')),
  CONSTRAINT ck_integration_status CHECK (status IN ('DISCONNECTED', 'CONFIGURED', 'CONNECTED', 'ERROR', 'REVOKED')),
  CONSTRAINT ck_integration_configuration_object CHECK (jsonb_typeof(configuration) = 'object'),
  CONSTRAINT ck_integration_credential_pair CHECK (
    (encrypted_credential IS NULL AND credential_key_id IS NULL)
    OR (encrypted_credential IS NOT NULL AND credential_key_id IS NOT NULL)
  )
);

CREATE INDEX ix_integration_connection_status
  ON integration_connection (organization_id, status, provider);

CREATE TABLE message_record (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  organization_id UUID NOT NULL REFERENCES organization (id),
  campaign_id UUID REFERENCES campaign (id),
  prospect_id UUID NOT NULL REFERENCES prospect (id),
  contact_id UUID REFERENCES contact (id),
  contact_channel_id UUID REFERENCES contact_channel (id),
  created_by UUID REFERENCES app_user (id),
  channel TEXT NOT NULL,
  direction TEXT NOT NULL,
  status TEXT NOT NULL,
  sending_block_reason TEXT,
  subject TEXT,
  body_text TEXT NOT NULL,
  body_html TEXT,
  provider TEXT NOT NULL,
  external_message_id TEXT,
  external_thread_id TEXT,
  idempotency_key TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_message_record_idempotency UNIQUE (organization_id, idempotency_key),
  CONSTRAINT ck_message_record_channel CHECK (channel IN ('EMAIL', 'WHATSAPP')),
  CONSTRAINT ck_message_record_direction CHECK (direction IN ('OUTBOUND', 'INBOUND')),
  CONSTRAINT ck_message_record_status CHECK (status IN (
    'DRAFT_CREATED', 'SIMULATED', 'BLOCKED_BY_KILL_SWITCH',
    'BLOCKED_BY_CONFIGURATION', 'BLOCKED_BY_EXCLUSION', 'BLOCKED_BY_POLICY',
    'PROVIDER_DRAFT_CREATED', 'PROVIDER_FAILED', 'RECEIVED'
  )),
  CONSTRAINT ck_message_record_provider CHECK (provider IN (
    'NOOP', 'FAKE', 'MANUAL', 'GMAIL', 'WHATSAPP_CLOUD'
  )),
  CONSTRAINT ck_message_record_body CHECK (length(body_text) BETWEEN 1 AND 100000)
);

CREATE INDEX ix_message_record_prospect
  ON message_record (organization_id, prospect_id, created_at DESC, id DESC);
CREATE INDEX ix_message_record_campaign
  ON message_record (organization_id, campaign_id, status);
CREATE INDEX ix_message_record_external
  ON message_record (organization_id, provider, external_thread_id)
  WHERE external_thread_id IS NOT NULL;

CREATE TABLE message_provider_attempt (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  message_id UUID NOT NULL REFERENCES message_record (id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  operation TEXT NOT NULL,
  result TEXT NOT NULL,
  http_status INTEGER,
  error_code TEXT,
  external_id TEXT,
  started_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_message_attempt_operation CHECK (operation IN ('CREATE_DRAFT', 'SIMULATE', 'SEND')),
  CONSTRAINT ck_message_attempt_result CHECK (result IN ('SUCCESS', 'BLOCKED', 'TRANSIENT_FAILURE', 'PERMANENT_FAILURE'))
);

CREATE INDEX ix_message_provider_attempt_message
  ON message_provider_attempt (organization_id, message_id, started_at DESC);
