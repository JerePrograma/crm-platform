package com.gestudio.crm.outbox;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "crm.outbox")
public record OutboxProperties(
    boolean enabled,
    @Min(1) @Max(100) int batchSize,
    Duration lease,
    Duration baseBackoff,
    Duration maxBackoff,
    @Min(1) @Max(20) int defaultMaxAttempts,
    @Min(1024) @Max(65536) int maxPayloadBytes) {

  public OutboxProperties {
    batchSize = batchSize == 0 ? 20 : batchSize;
    lease = lease == null ? Duration.ofMinutes(2) : lease;
    baseBackoff = baseBackoff == null ? Duration.ofSeconds(5) : baseBackoff;
    maxBackoff = maxBackoff == null ? Duration.ofMinutes(15) : maxBackoff;
    defaultMaxAttempts = defaultMaxAttempts == 0 ? 5 : defaultMaxAttempts;
    maxPayloadBytes = maxPayloadBytes == 0 ? 65536 : maxPayloadBytes;
    if (lease.isZero() || lease.isNegative()) {
      throw new IllegalArgumentException("Outbox lease must be positive");
    }
    if (baseBackoff.isNegative() || maxBackoff.isNegative()) {
      throw new IllegalArgumentException("Outbox backoff cannot be negative");
    }
  }
}
