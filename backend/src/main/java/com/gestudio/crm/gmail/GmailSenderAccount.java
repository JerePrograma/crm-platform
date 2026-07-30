package com.gestudio.crm.gmail;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

record GmailSenderAccount(
    UUID id,
    long version,
    UUID organizationId,
    String emailAddress,
    String normalizedEmail,
    String displayName,
    GmailSenderAccountStatus status,
    boolean defaultAccount,
    Set<String> grantedScopes,
    GmailTokenCipher.EncryptedSecret encryptedRefreshToken,
    UUID connectedBy,
    Instant connectedAt,
    Instant verifiedAt,
    Instant revokedAt,
    String lastErrorSummary,
    int dailyLimit,
    int minIntervalSeconds,
    Instant nextSendAt) {

  @Override
  public String toString() {
    return "GmailSenderAccount[id="
        + id
        + ",organizationId="
        + organizationId
        + ",emailAddress="
        + emailAddress
        + ",status="
        + status
        + ",credential=REDACTED]";
  }
}
