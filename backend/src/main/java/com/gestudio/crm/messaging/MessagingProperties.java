package com.gestudio.crm.messaging;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "messaging")
public record MessagingProperties(
    @NotBlank String emailMode,
    @NotBlank String whatsappMode,
    boolean realNetworkAllowed,
    Duration timeout,
    Gmail gmail,
    WhatsApp whatsapp) {

  public MessagingProperties {
    timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
  }

  public record Gmail(String baseUrl, String accessToken, String scopes) {}

  public record WhatsApp(
      String baseUrl,
      String apiVersion,
      String phoneNumberId,
      String businessAccountId,
      String accessToken,
      @Min(1) int maxTextLength) {}
}
