package com.gestudio.crm.inbound;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "crm.inbound")
public record InboundProperties(
    boolean enabled,
    String fakeWebhookSecret,
    Duration signatureWindow,
    @Min(1024) @Max(65536) int maxPayloadBytes,
    @Min(1) @Max(1000) int requestsPerMinute) {

  public InboundProperties {
    fakeWebhookSecret = fakeWebhookSecret == null ? "" : fakeWebhookSecret;
    signatureWindow = signatureWindow == null ? Duration.ofMinutes(5) : signatureWindow;
    maxPayloadBytes = maxPayloadBytes == 0 ? 32768 : maxPayloadBytes;
    requestsPerMinute = requestsPerMinute == 0 ? 60 : requestsPerMinute;
    if (signatureWindow.isZero() || signatureWindow.isNegative()) {
      throw new IllegalArgumentException("Inbound signature window must be positive");
    }
  }

  public boolean configured() {
    return enabled && !fakeWebhookSecret.isBlank();
  }
}
