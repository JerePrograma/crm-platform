package com.gestudio.crm.gmail;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SenderAccountView(
    UUID id,
    long version,
    String provider,
    String emailAddress,
    String displayName,
    GmailSenderAccountStatus status,
    boolean defaultAccount,
    Set<String> grantedScopes,
    Instant connectedAt,
    Instant verifiedAt,
    Instant revokedAt,
    String lastErrorSummary,
    int dailyLimit,
    int minIntervalSeconds,
    Instant nextSendAt) {}
