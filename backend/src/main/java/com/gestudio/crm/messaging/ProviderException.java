package com.gestudio.crm.messaging;

public class ProviderException extends RuntimeException {
  private final boolean retryable;
  private final String code;

  public ProviderException(String message, String code, boolean retryable) {
    super(message);
    this.code = code;
    this.retryable = retryable;
  }

  public boolean retryable() {
    return retryable;
  }

  public String code() {
    return code;
  }
}
