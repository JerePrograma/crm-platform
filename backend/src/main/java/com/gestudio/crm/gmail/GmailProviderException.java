package com.gestudio.crm.gmail;

import java.time.Instant;

public final class GmailProviderException extends RuntimeException {

  private final Category category;
  private final Integer httpStatus;
  private final Instant retryAt;

  public GmailProviderException(
      Category category, Integer httpStatus, Instant retryAt, String safeMessage) {
    super(safeMessage);
    this.category = category;
    this.httpStatus = httpStatus;
    this.retryAt = retryAt;
  }

  public Category category() {
    return category;
  }

  public Integer httpStatus() {
    return httpStatus;
  }

  public Instant retryAt() {
    return retryAt;
  }

  public boolean retryable() {
    return category == Category.RATE_LIMIT || category == Category.RETRYABLE;
  }

  public enum Category {
    VALIDATION,
    REAUTH_REQUIRED,
    INSUFFICIENT_SCOPE,
    QUOTA,
    RATE_LIMIT,
    RETRYABLE,
    AMBIGUOUS,
    PERMANENT,
    INVALID_RESPONSE
  }
}
