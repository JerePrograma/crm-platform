package com.gestudio.crm.gmail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public final class GmailAccessToken {

  private final String value;
  private final Instant expiresAt;

  GmailAccessToken(String value, Instant expiresAt) {
    this.value = value;
    this.expiresAt = expiresAt;
  }

  @JsonIgnore
  public String value() {
    return value;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  @Override
  public String toString() {
    return "GmailAccessToken[REDACTED,expiresAt=" + expiresAt + "]";
  }
}
