package com.gestudio.crm.outbox;

public enum OutboxErrorCategory {
  RETRYABLE,
  DEFERRED,
  NON_RETRYABLE,
  POLICY_BLOCK,
  CONFIGURATION_BLOCK,
  AMBIGUOUS,
  CANCELLED,
  DUPLICATE
}
