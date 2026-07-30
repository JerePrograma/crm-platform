package com.gestudio.crm.messaging;

import com.gestudio.crm.gmail.CampaignOnlyGmailEmailProvider;
import com.gestudio.crm.gmail.GmailDeliveryProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class MessagingProviderConfiguration {

  @Bean
  HttpClient messagingHttpClient(MessagingProperties properties) {
    return HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
  }

  @Bean
  EmailProvider emailProvider(
      MessagingProperties properties,
      HttpClient httpClient,
      ObjectMapper objectMapper,
      GmailDeliveryProperties gmailDeliveryProperties) {
    return buildEmailProvider(properties, httpClient, objectMapper, gmailDeliveryProperties);
  }

  EmailProvider emailProvider(
      MessagingProperties properties, HttpClient httpClient, ObjectMapper objectMapper) {
    return buildEmailProvider(properties, httpClient, objectMapper, null);
  }

  private EmailProvider buildEmailProvider(
      MessagingProperties properties,
      HttpClient httpClient,
      ObjectMapper objectMapper,
      GmailDeliveryProperties gmailDeliveryProperties) {
    return switch (properties.emailMode().toUpperCase(java.util.Locale.ROOT)) {
      case "NOOP" -> new NoopEmailProvider();
      case "FAKE" -> new FakeEmailProvider();
      case "GMAIL_DRAFT_ONLY" -> {
        requireRealNetwork(properties, "Gmail");
        var gmail = properties.gmail();
        yield new GmailEmailProvider(
            httpClient,
            objectMapper,
            URI.create(gmail.baseUrl()),
            gmail.accessToken(),
            scopes(gmail.scopes()),
            properties.timeout(),
            false);
      }
      case "GMAIL_LIVE" -> {
        requireRealNetwork(properties, "Gmail");
        if (gmailDeliveryProperties == null) {
          throw new IllegalStateException("Gmail live configuration is unavailable");
        }
        gmailDeliveryProperties.requireLiveConfigured();
        yield new CampaignOnlyGmailEmailProvider();
      }
      default -> throw new IllegalStateException("Unsupported email provider mode");
    };
  }

  @Bean
  WhatsAppProvider whatsAppProvider(
      MessagingProperties properties,
      com.gestudio.crm.settings.SendingProperties sendingProperties,
      HttpClient httpClient,
      ObjectMapper objectMapper) {
    return switch (properties.whatsappMode().toUpperCase(java.util.Locale.ROOT)) {
      case "NOOP", "DEEPLINK_ONLY" -> new NoopWhatsAppProvider();
      case "FAKE" -> new FakeWhatsAppProvider();
      case "WHATSAPP_CLOUD" -> {
        requireRealNetwork(properties, "WhatsApp Cloud");
        var whatsApp = properties.whatsapp();
        yield new WhatsAppCloudProvider(
            httpClient,
            objectMapper,
            URI.create(whatsApp.baseUrl()),
            whatsApp.apiVersion(),
            whatsApp.phoneNumberId(),
            whatsApp.accessToken(),
            properties.timeout(),
            whatsApp.maxTextLength(),
            !sendingProperties.blocksRealSending());
      }
      default -> throw new IllegalStateException("Unsupported WhatsApp provider mode");
    };
  }

  private void requireRealNetwork(MessagingProperties properties, String provider) {
    if (!properties.realNetworkAllowed()) {
      throw new IllegalStateException(
          provider + " cannot initialize while real network is disabled");
    }
  }

  private Set<String> scopes(String value) {
    return value == null
        ? Set.of()
        : Arrays.stream(value.split("[ ,]+"))
            .filter(item -> !item.isBlank())
            .collect(Collectors.toUnmodifiableSet());
  }
}
