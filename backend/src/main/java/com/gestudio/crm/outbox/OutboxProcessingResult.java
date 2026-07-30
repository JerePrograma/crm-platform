package com.gestudio.crm.outbox;

import java.time.Instant;

public record OutboxProcessingResult(
    boolean succeeded,
    OutboxErrorCategory category,
    String code,
    String summary,
    String resultSummary,
    Instant retryAt) {

  public static OutboxProcessingResult success(String resultSummary) {
    return new OutboxProcessingResult(true, null, null, null, resultSummary, null);
  }

  public static OutboxProcessingResult failure(
      OutboxErrorCategory category, String code, String summary) {
    return new OutboxProcessingResult(false, category, code, summary, null, null);
  }

  public static OutboxProcessingResult retry(String code, String summary, Instant retryAt) {
    return new OutboxProcessingResult(
        false, OutboxErrorCategory.RETRYABLE, code, summary, null, retryAt);
  }

  public static OutboxProcessingResult defer(String code, String summary, Instant retryAt) {
    return new OutboxProcessingResult(
        false, OutboxErrorCategory.DEFERRED, code, summary, null, retryAt);
  }
}
