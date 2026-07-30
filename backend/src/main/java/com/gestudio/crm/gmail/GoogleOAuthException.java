package com.gestudio.crm.gmail;

public final class GoogleOAuthException extends RuntimeException {

  private final Code code;

  public GoogleOAuthException(Code code, String safeMessage) {
    super(safeMessage);
    this.code = code;
  }

  public Code code() {
    return code;
  }

  public enum Code {
    INVALID_GRANT,
    INSUFFICIENT_SCOPE,
    INVALID_RESPONSE,
    REMOTE_FAILURE,
    AMBIGUOUS
  }
}
