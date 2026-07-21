package com.gestudio.crm.messaging;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class GmailEmailProvider implements EmailProvider {

  public static final String COMPOSE_SCOPE = "https://www.googleapis.com/auth/gmail.compose";
  public static final String SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send";
  public static final String READ_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final URI baseUri;
  private final String accessToken;
  private final Duration timeout;
  private final boolean sendingAllowed;

  public GmailEmailProvider(
      HttpClient httpClient,
      ObjectMapper objectMapper,
      URI baseUri,
      String accessToken,
      Set<String> scopes,
      Duration timeout,
      boolean sendingAllowed) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.baseUri = requireEndpoint(baseUri);
    this.accessToken = required(accessToken, "Gmail access token");
    this.timeout = timeout;
    this.sendingAllowed = sendingAllowed;
    if (!scopes.contains(COMPOSE_SCOPE)) {
      throw new IllegalArgumentException("Gmail draft mode requires gmail.compose scope");
    }
    if (sendingAllowed && !scopes.contains(SEND_SCOPE)) {
      throw new IllegalArgumentException("Gmail sending requires gmail.send scope");
    }
  }

  @Override
  public String name() {
    return "GMAIL";
  }

  @Override
  public ProviderResult createDraft(OutboundMessage message) {
    String body =
        json(java.util.Map.of("message", java.util.Map.of("raw", base64Url(mime(message)))));
    JsonNode response = post("/gmail/v1/users/me/drafts", body);
    String draftId = text(response, "id");
    String threadId = response.path("message").path("threadId").asText(null);
    return new ProviderResult("PROVIDER_DRAFT_CREATED", name(), draftId, threadId);
  }

  @Override
  public ProviderResult send(OutboundMessage message) {
    if (!sendingAllowed) {
      throw new ProviderException("Gmail adapter is draft-only", "SEND_DISABLED", false);
    }
    String body = json(java.util.Map.of("raw", base64Url(mime(message))));
    JsonNode response = post("/gmail/v1/users/me/messages/send", body);
    return new ProviderResult(
        "SENT", name(), text(response, "id"), response.path("threadId").asText(null));
  }

  private JsonNode post(String path, String body) {
    HttpRequest request =
        HttpRequest.newBuilder(baseUri.resolve(path))
            .timeout(timeout)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      requireSuccess(response.statusCode(), "GMAIL_HTTP_" + response.statusCode());
      return objectMapper.readTree(response.body());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ProviderException("Gmail request interrupted", "GMAIL_INTERRUPTED", true);
    } catch (java.io.IOException | JacksonException exception) {
      throw new ProviderException("Gmail request failed", "GMAIL_IO", true);
    }
  }

  private String mime(OutboundMessage message) {
    String recipient = required(message.recipient(), "Email recipient");
    if (!EMAIL.matcher(recipient).matches()
        || recipient.contains("\r")
        || recipient.contains("\n")) {
      throw new IllegalArgumentException("Invalid email recipient");
    }
    String subject = required(message.subject(), "Email subject");
    if (subject.contains("\r") || subject.contains("\n")) {
      throw new IllegalArgumentException("Invalid email subject");
    }
    String encodedSubject =
        "=?UTF-8?B?"
            + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8))
            + "?=";
    return "To: "
        + recipient
        + "\r\nSubject: "
        + encodedSubject
        + "\r\nMIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n"
        + message.textBody();
  }

  private String base64Url(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Gmail payload cannot be serialized", exception);
    }
  }

  private String text(JsonNode node, String field) {
    String value = node.path(field).asText(null);
    if (value == null || value.isBlank()) {
      throw new ProviderException(
          "Gmail response omitted " + field, "GMAIL_INVALID_RESPONSE", false);
    }
    return value;
  }

  private void requireSuccess(int status, String code) {
    if (status >= 200 && status < 300) {
      return;
    }
    throw new ProviderException(
        "Gmail returned HTTP " + status, code, status == 429 || status >= 500);
  }

  private URI requireEndpoint(URI uri) {
    if (uri == null
        || !("https".equalsIgnoreCase(uri.getScheme())
            || ("http".equalsIgnoreCase(uri.getScheme())
                && Set.of("localhost", "127.0.0.1").contains(uri.getHost())))) {
      throw new IllegalArgumentException("Gmail endpoint must use HTTPS or test loopback");
    }
    return uri;
  }

  private String required(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " is required");
    }
    return value.trim();
  }
}
