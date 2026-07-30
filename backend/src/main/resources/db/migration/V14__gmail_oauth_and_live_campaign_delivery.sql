ALTER TABLE integration_connection
  DROP CONSTRAINT uk_integration_connection_org_provider,
  DROP CONSTRAINT ck_integration_status,
  ADD COLUMN email_address TEXT,
  ADD COLUMN normalized_email TEXT,
  ADD COLUMN display_name TEXT,
  ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN granted_scopes TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  ADD COLUMN credential_nonce BYTEA,
  ADD COLUMN connected_by UUID REFERENCES app_user (id),
  ADD COLUMN verified_at TIMESTAMPTZ,
  ADD COLUMN revoked_at TIMESTAMPTZ,
  ADD COLUMN last_error_at TIMESTAMPTZ,
  ADD COLUMN last_error_summary TEXT,
  ADD COLUMN daily_limit INTEGER NOT NULL DEFAULT 10,
  ADD COLUMN min_interval_seconds INTEGER NOT NULL DEFAULT 60,
  ADD COLUMN next_send_at TIMESTAMPTZ,
  ADD CONSTRAINT ck_integration_status CHECK (
    status IN ('DISCONNECTED', 'CONFIGURED', 'CONNECTED', 'REAUTH_REQUIRED', 'ERROR', 'REVOKED')
  ),
  ADD CONSTRAINT ck_integration_email_pair CHECK (
    (email_address IS NULL AND normalized_email IS NULL)
    OR (email_address IS NOT NULL AND normalized_email IS NOT NULL
      AND normalized_email = lower(normalized_email))
  ),
  ADD CONSTRAINT ck_integration_nonce_credential CHECK (
    credential_nonce IS NULL OR encrypted_credential IS NOT NULL
  ),
  ADD CONSTRAINT ck_integration_daily_limit CHECK (daily_limit BETWEEN 1 AND 10000),
  ADD CONSTRAINT ck_integration_min_interval CHECK (min_interval_seconds BETWEEN 0 AND 86400),
  ADD CONSTRAINT ck_integration_error_summary CHECK (
    last_error_summary IS NULL OR char_length(last_error_summary) <= 500
  );

UPDATE integration_connection
SET status = 'REAUTH_REQUIRED',
    last_error_at = now(),
    last_error_summary = 'Reconnect required for OAuth Gmail sender account',
    updated_at = now(),
    version = version + 1
WHERE provider = 'GMAIL' AND status = 'CONNECTED';

ALTER TABLE integration_connection
  ADD CONSTRAINT ck_integration_scopes_nonempty CHECK (
    provider <> 'GMAIL' OR status <> 'CONNECTED' OR cardinality(granted_scopes) > 0
  );

CREATE UNIQUE INDEX uk_integration_connection_org_provider_email
  ON integration_connection (organization_id, provider, normalized_email)
  WHERE normalized_email IS NOT NULL;
CREATE UNIQUE INDEX uk_integration_connection_org_provider_legacy
  ON integration_connection (organization_id, provider)
  WHERE normalized_email IS NULL;
CREATE UNIQUE INDEX uk_integration_connection_default
  ON integration_connection (organization_id, provider)
  WHERE is_default;
CREATE UNIQUE INDEX uk_integration_connection_org_id
  ON integration_connection (organization_id, id);
CREATE INDEX ix_integration_connection_email
  ON integration_connection (organization_id, provider, normalized_email, status);

CREATE TABLE gmail_oauth_state (
  id UUID PRIMARY KEY,
  state_hash CHAR(64) NOT NULL UNIQUE,
  organization_id UUID NOT NULL REFERENCES organization (id),
  user_id UUID NOT NULL REFERENCES app_user (id),
  session_hash CHAR(64) NOT NULL,
  reconnect_account_id UUID,
  expires_at TIMESTAMPTZ NOT NULL,
  consumed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_gmail_oauth_reconnect_account
    FOREIGN KEY (organization_id, reconnect_account_id)
    REFERENCES integration_connection (organization_id, id),
  CONSTRAINT ck_gmail_oauth_state_hash CHECK (state_hash ~ '^[0-9a-f]{64}$'),
  CONSTRAINT ck_gmail_oauth_session_hash CHECK (session_hash ~ '^[0-9a-f]{64}$'),
  CONSTRAINT ck_gmail_oauth_expiry CHECK (expires_at > created_at),
  CONSTRAINT ck_gmail_oauth_consumed CHECK (consumed_at IS NULL OR consumed_at >= created_at)
);

CREATE INDEX ix_gmail_oauth_state_expiry
  ON gmail_oauth_state (expires_at) WHERE consumed_at IS NULL;
CREATE INDEX ix_gmail_oauth_state_actor
  ON gmail_oauth_state (organization_id, user_id, created_at DESC);

CREATE FUNCTION crm_smallint_array_unique(input_values SMALLINT[])
RETURNS BOOLEAN
LANGUAGE SQL
IMMUTABLE
STRICT
AS $$
  SELECT cardinality(input_values) =
    (SELECT count(DISTINCT item) FROM unnest(input_values) AS item)
$$;

ALTER TABLE campaign
  ADD COLUMN execution_mode TEXT NOT NULL DEFAULT 'SIMULATION',
  ADD COLUMN sender_account_id UUID,
  ADD COLUMN timezone TEXT NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
  ADD COLUMN operating_window_start TIME NOT NULL DEFAULT TIME '09:30',
  ADD COLUMN operating_window_end TIME NOT NULL DEFAULT TIME '17:30',
  ADD COLUMN business_days SMALLINT[] NOT NULL DEFAULT ARRAY[1, 2, 3, 4, 5]::SMALLINT[],
  ADD COLUMN minimum_interval_seconds INTEGER NOT NULL DEFAULT 60,
  ADD COLUMN max_attempts INTEGER NOT NULL DEFAULT 5,
  ADD COLUMN stop_configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
  ADD COLUMN reply_to TEXT,
  ADD COLUMN approval_fingerprint CHAR(64),
  ADD COLUMN executed_by UUID REFERENCES app_user (id),
  ADD COLUMN started_at TIMESTAMPTZ,
  ADD COLUMN paused_at TIMESTAMPTZ,
  ADD COLUMN completed_at TIMESTAMPTZ,
  ADD COLUMN cancelled_at TIMESTAMPTZ,
  ADD CONSTRAINT fk_campaign_sender_account
    FOREIGN KEY (organization_id, sender_account_id)
    REFERENCES integration_connection (organization_id, id),
  ADD CONSTRAINT ck_campaign_execution_mode CHECK (execution_mode IN ('SIMULATION', 'LIVE')),
  ADD CONSTRAINT ck_campaign_live_sender CHECK (
    execution_mode = 'SIMULATION' OR (channel = 'EMAIL' AND sender_account_id IS NOT NULL)
  ),
  ADD CONSTRAINT ck_campaign_operating_window CHECK (operating_window_start < operating_window_end),
  ADD CONSTRAINT ck_campaign_business_days CHECK (
    business_days <@ ARRAY[1, 2, 3, 4, 5, 6, 7]::SMALLINT[]
      AND cardinality(business_days) BETWEEN 1 AND 7
      AND crm_smallint_array_unique(business_days)
  ),
  ADD CONSTRAINT ck_campaign_minimum_interval CHECK (minimum_interval_seconds BETWEEN 0 AND 86400),
  ADD CONSTRAINT ck_campaign_max_attempts CHECK (max_attempts BETWEEN 1 AND 20),
  ADD CONSTRAINT ck_campaign_stop_configuration CHECK (jsonb_typeof(stop_configuration) = 'object'),
  ADD CONSTRAINT ck_campaign_reply_to CHECK (
    reply_to IS NULL OR (char_length(reply_to) BETWEEN 3 AND 320 AND reply_to !~ '[\r\n]')
  ),
  ADD CONSTRAINT ck_campaign_approval_fingerprint CHECK (
    approval_fingerprint IS NULL OR approval_fingerprint ~ '^[0-9a-f]{64}$'
  );

CREATE INDEX ix_campaign_live_schedule
  ON campaign (organization_id, execution_mode, status, scheduled_at)
  WHERE execution_mode = 'LIVE';
CREATE INDEX ix_campaign_sender
  ON campaign (organization_id, sender_account_id, status)
  WHERE sender_account_id IS NOT NULL;

ALTER TABLE message_record
  DROP CONSTRAINT ck_message_record_status,
  ADD COLUMN sender_account_id UUID,
  ADD COLUMN audience_recipient_id UUID REFERENCES campaign_audience_recipient (id),
  ADD COLUMN accepted_at TIMESTAMPTZ,
  ADD COLUMN last_http_status INTEGER,
  ADD COLUMN result_category TEXT,
  ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN next_attempt_at TIMESTAMPTZ,
  ADD COLUMN last_error_summary TEXT,
  ADD COLUMN correlation_id TEXT,
  ADD COLUMN transmission_started_at TIMESTAMPTZ,
  ADD CONSTRAINT fk_message_sender_account
    FOREIGN KEY (organization_id, sender_account_id)
    REFERENCES integration_connection (organization_id, id),
  ADD CONSTRAINT ck_message_record_status CHECK (status IN (
    'DRAFT_CREATED', 'SIMULATED', 'BLOCKED_BY_KILL_SWITCH',
    'BLOCKED_BY_CONFIGURATION', 'BLOCKED_BY_EXCLUSION', 'BLOCKED_BY_POLICY',
    'PROVIDER_DRAFT_CREATED', 'PROVIDER_FAILED', 'RECEIVED',
    'PENDING', 'SCHEDULED', 'PROCESSING', 'ACCEPTED_BY_GMAIL', 'RETRYABLE',
    'AMBIGUOUS', 'FAILED_PERMANENT', 'CANCELLED', 'SKIPPED'
  )),
  ADD CONSTRAINT ck_message_live_sender CHECK (
    status NOT IN ('PENDING', 'SCHEDULED', 'PROCESSING', 'ACCEPTED_BY_GMAIL',
      'RETRYABLE', 'AMBIGUOUS', 'FAILED_PERMANENT') OR sender_account_id IS NOT NULL
  ),
  ADD CONSTRAINT ck_message_attempt_count CHECK (attempt_count BETWEEN 0 AND 20),
  ADD CONSTRAINT ck_message_http_status CHECK (
    last_http_status IS NULL OR last_http_status BETWEEN 100 AND 599
  ),
  ADD CONSTRAINT ck_message_error_summary CHECK (
    last_error_summary IS NULL OR char_length(last_error_summary) <= 500
  ),
  ADD CONSTRAINT ck_message_correlation CHECK (
    correlation_id IS NULL OR char_length(correlation_id) BETWEEN 1 AND 128
  ),
  ADD CONSTRAINT ck_message_acceptance CHECK (
    status <> 'ACCEPTED_BY_GMAIL'
      OR (accepted_at IS NOT NULL AND external_message_id IS NOT NULL AND provider = 'GMAIL')
  );

CREATE UNIQUE INDEX uk_message_record_campaign_audience
  ON message_record (organization_id, campaign_id, audience_recipient_id)
  WHERE campaign_id IS NOT NULL AND audience_recipient_id IS NOT NULL;
CREATE INDEX ix_message_record_campaign_delivery
  ON message_record (organization_id, campaign_id, status, next_attempt_at, created_at);

CREATE UNIQUE INDEX uk_campaign_org_id ON campaign (organization_id, id);
CREATE UNIQUE INDEX uk_message_record_org_id ON message_record (organization_id, id);
CREATE UNIQUE INDEX uk_contact_channel_org_id ON contact_channel (organization_id, id);

ALTER TABLE message_provider_attempt
  DROP CONSTRAINT ck_message_attempt_result,
  ADD COLUMN retry_after TIMESTAMPTZ,
  ADD COLUMN result_category TEXT,
  ADD COLUMN correlation_id TEXT,
  ADD COLUMN response_summary TEXT,
  ADD CONSTRAINT ck_message_attempt_result CHECK (
    result IN ('SUCCESS', 'BLOCKED', 'TRANSIENT_FAILURE', 'PERMANENT_FAILURE', 'AMBIGUOUS')
  ),
  ADD CONSTRAINT ck_message_attempt_http_status CHECK (
    http_status IS NULL OR http_status BETWEEN 100 AND 599
  ),
  ADD CONSTRAINT ck_message_attempt_correlation CHECK (
    correlation_id IS NULL OR char_length(correlation_id) BETWEEN 1 AND 128
  ),
  ADD CONSTRAINT ck_message_attempt_summary CHECK (
    response_summary IS NULL OR char_length(response_summary) <= 500
  );

CREATE TABLE unsubscribe_token (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization (id),
  campaign_id UUID NOT NULL,
  message_id UUID NOT NULL,
  contact_channel_id UUID NOT NULL,
  token_hash CHAR(64) NOT NULL,
  key_id TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  expires_at TIMESTAMPTZ,
  used_at TIMESTAMPTZ,
  CONSTRAINT fk_unsubscribe_campaign
    FOREIGN KEY (organization_id, campaign_id) REFERENCES campaign (organization_id, id),
  CONSTRAINT fk_unsubscribe_message
    FOREIGN KEY (organization_id, message_id) REFERENCES message_record (organization_id, id),
  CONSTRAINT fk_unsubscribe_channel
    FOREIGN KEY (organization_id, contact_channel_id) REFERENCES contact_channel (organization_id, id),
  CONSTRAINT uk_unsubscribe_token_hash UNIQUE (token_hash),
  CONSTRAINT uk_unsubscribe_message UNIQUE (organization_id, message_id),
  CONSTRAINT ck_unsubscribe_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
  CONSTRAINT ck_unsubscribe_key_id CHECK (char_length(key_id) BETWEEN 1 AND 64),
  CONSTRAINT ck_unsubscribe_expiry CHECK (expires_at IS NULL OR expires_at > created_at),
  CONSTRAINT ck_unsubscribe_used CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE INDEX ix_unsubscribe_token_lookup ON unsubscribe_token (id, token_hash);
CREATE INDEX ix_unsubscribe_token_channel
  ON unsubscribe_token (organization_id, contact_channel_id, used_at);

CREATE TABLE delivery_daily_ledger (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  organization_id UUID NOT NULL REFERENCES organization (id),
  scope_type TEXT NOT NULL,
  scope_id UUID NOT NULL,
  local_date DATE NOT NULL,
  reserved_count INTEGER NOT NULL DEFAULT 0,
  accepted_count INTEGER NOT NULL DEFAULT 0,
  released_count INTEGER NOT NULL DEFAULT 0,
  last_send_at TIMESTAMPTZ,
  lease_message_id UUID REFERENCES message_record (id),
  lease_expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_delivery_daily_scope UNIQUE (organization_id, scope_type, scope_id, local_date),
  CONSTRAINT ck_delivery_daily_scope CHECK (scope_type IN ('ORGANIZATION', 'SENDER', 'CAMPAIGN')),
  CONSTRAINT ck_delivery_daily_counts CHECK (
    reserved_count >= 0 AND accepted_count >= 0 AND released_count >= 0
      AND accepted_count <= reserved_count AND released_count <= reserved_count
  ),
  CONSTRAINT ck_delivery_daily_lease CHECK (
    (lease_message_id IS NULL AND lease_expires_at IS NULL)
    OR (lease_message_id IS NOT NULL AND lease_expires_at IS NOT NULL)
  )
);

CREATE INDEX ix_delivery_daily_active_lease
  ON delivery_daily_ledger (lease_expires_at)
  WHERE lease_expires_at IS NOT NULL;

CREATE TABLE global_contact_suppression (
  id UUID PRIMARY KEY,
  channel_type TEXT NOT NULL,
  value_hash CHAR(64) NOT NULL,
  reason TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_global_contact_suppression UNIQUE (channel_type, value_hash),
  CONSTRAINT ck_global_suppression_channel CHECK (channel_type IN ('EMAIL', 'PHONE', 'WHATSAPP', 'WEBSITE')),
  CONSTRAINT ck_global_suppression_hash CHECK (value_hash ~ '^[0-9a-f]{64}$'),
  CONSTRAINT ck_global_suppression_reason CHECK (char_length(reason) BETWEEN 1 AND 200)
);
