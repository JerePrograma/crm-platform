package com.gestudio.crm.outbox;

public enum OutboxErrorCategory {
  RETRYABLE,
  NON_RETRYABLE,
  POLICY_BLOCK,
  CONFIGURATION_BLOCK,
  CANCELLED,
  DUPLICATE
}
