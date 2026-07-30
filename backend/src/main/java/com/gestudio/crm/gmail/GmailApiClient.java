package com.gestudio.crm.gmail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class GmailApiClient {

  private static final Duration MAX_RETRY_AFTER = Duration.ofHours(24);

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final GmailDeliveryProperties properties;
  private final GmailMimeBuilder mimeBuilder;
  private final Clock clock;

  public GmailApiClient(
      HttpClient httpClient,
      ObjectMapper objectMapper,
      GmailDeliveryProperties properties,
      GmailMimeBuilder mimeBuilder,
      Clock clock) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.mimeBuilder = mimeBuilder;
    this.clock = clock;
  }

  public SendResult send(String accessToken, GmailMimeBuilder.Message message) {
    if (accessToken == null || accessToken.isBlank()) {
      throw failure(
          GmailProviderException.Category.REAUTH_REQUIRED,
          null,
          null,
          "Gmail access token is unavailable");
    }
    String body;
    try {
      body = objectMapper.writeValueAsString(Map.of("raw", mimeBuilder.raw(message)));
    } catch (IllegalArgumentException | JacksonException exception) {
      throw failure(
          GmailProviderException.Category.VALIDATION, null, null, "Gmail message is invalid");
    }
    URI endpoint = properties.gmailApiBaseUri().resolve("/gmail/v1/users/me/messages/send");
    HttpRequest request =
        HttpRequest.newBuilder(endpoint)
            .timeout(properties.httpTimeout())
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
    HttpResponse<String> response;
    try {
      response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (HttpConnectTimeoutException | ConnectException exception) {
      throw failure(
          GmailProviderException.Category.RETRYABLE,
          null,
          null,
          "Gmail connection failed before transmission");
    } catch (HttpTimeoutException exception) {
      throw failure(
          GmailProviderException.Category.AMBIGUOUS,
          null,
          null,
          "Gmail request timed out with an unknown result");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw failure(
          GmailProviderException.Category.AMBIGUOUS,
          null,
          null,
          "Gmail request was interrupted with an unknown result");
    } catch (IOException exception) {
      throw failure(
          GmailProviderException.Category.AMBIGUOUS,
          null,
          null,
          "Gmail connection closed with an unknown result");
    }
    int status = response.statusCode();
    if (status >= 200 && status < 300) {
      try {
        JsonNode json = objectMapper.readTree(response.body());
        String providerMessageId = requiredText(json, "id");
        return new SendResult(
            providerMessageId, json.path("threadId").asText(null), status, clock.instant());
      } catch (JacksonException | IllegalArgumentException exception) {
        throw failure(
            GmailProviderException.Category.AMBIGUOUS,
            status,
            null,
            "Gmail accepted the request but returned an invalid response");
      }
    }
    throw httpFailure(status, response);
  }

  private GmailProviderException httpFailure(int status, HttpResponse<String> response) {
    return switch (status) {
      case 400 ->
          failure(
              GmailProviderException.Category.PERMANENT,
              status,
              null,
              "Gmail rejected the message");
      case 401 ->
          failure(
              GmailProviderException.Category.REAUTH_REQUIRED,
              status,
              null,
              "Gmail authorization must be renewed");
      case 403 ->
          failure(
              quota(response.body())
                  ? GmailProviderException.Category.QUOTA
                  : GmailProviderException.Category.INSUFFICIENT_SCOPE,
              status,
              null,
              quota(response.body())
                  ? "Gmail quota is exhausted"
                  : "Gmail permission is insufficient");
      case 429 ->
          failure(
              GmailProviderException.Category.RATE_LIMIT,
              status,
              retryAt(response),
              "Gmail rate limit was reached");
      default ->
          status >= 500
              ? failure(
                  GmailProviderException.Category.RETRYABLE,
                  status,
                  retryAt(response),
                  "Gmail is temporarily unavailable")
              : failure(
                  GmailProviderException.Category.PERMANENT,
                  status,
                  null,
                  "Gmail rejected the request");
    };
  }

  private boolean quota(String body) {
    if (body == null || body.length() > 64_000) {
      return false;
    }
    String normalized = body.toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("quota") || normalized.contains("dailylimitexceeded");
  }

  private Instant retryAt(HttpResponse<?> response) {
    String value = response.headers().firstValue("Retry-After").orElse(null);
    if (value == null) {
      return null;
    }
    Instant now = clock.instant();
    try {
      long seconds = Long.parseLong(value.trim());
      return now.plus(
          Duration.ofSeconds(Math.max(0, Math.min(seconds, MAX_RETRY_AFTER.toSeconds()))));
    } catch (NumberFormatException ignored) {
      try {
        Instant requested =
            ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        if (requested.isBefore(now)) {
          return now;
        }
        Instant maximum = now.plus(MAX_RETRY_AFTER);
        return requested.isAfter(maximum) ? maximum : requested;
      } catch (DateTimeParseException invalidDate) {
        return null;
      }
    }
  }

  private String requiredText(JsonNode json, String field) {
    String value = json.path(field).asText(null);
    if (value == null || value.isBlank() || value.length() > 512) {
      throw new IllegalArgumentException("Missing Gmail response identifier");
    }
    return value;
  }

  private GmailProviderException failure(
      GmailProviderException.Category category,
      Integer status,
      Instant retryAt,
      String safeMessage) {
    return new GmailProviderException(category, status, retryAt, safeMessage);
  }

  public record SendResult(
      String providerMessageId, String threadId, int httpStatus, Instant acceptedAt) {}
}
