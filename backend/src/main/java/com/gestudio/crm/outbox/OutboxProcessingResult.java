package com.gestudio.crm.outbox;

public record OutboxProcessingResult(
    boolean succeeded,
    OutboxErrorCategory category,
    String code,
    String summary,
    String resultSummary) {

  public static OutboxProcessingResult success(String resultSummary) {
    return new OutboxProcessingResult(true, null, null, null, resultSummary);
  }

  public static OutboxProcessingResult failure(
      OutboxErrorCategory category, String code, String summary) {
    return new OutboxProcessingResult(false, category, code, summary, null);
  }
}
