package com.gestudio.crm.outbox;

public enum OutboxStatus {
  PENDING,
  PROCESSING,
  SUCCEEDED,
  RETRY,
  DEAD,
  CANCELLED,
  BLOCKED
}
