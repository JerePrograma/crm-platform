package com.gestudio.crm.messaging;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class WhatsAppCloudProvider implements WhatsAppProvider {

  private static final Pattern VERSION = Pattern.compile("^v\\d+\\.\\d+$");
  private static final Pattern DIGITS = Pattern.compile("^\\d{6,20}$");

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final URI baseUri;
  private final String apiVersion;
  private final String phoneNumberId;
  private final String accessToken;
  private final Duration timeout;
  private final int maxTextLength;
  private final boolean sendingAllowed;

  public WhatsAppCloudProvider(
      HttpClient httpClient,
      ObjectMapper objectMapper,
      URI baseUri,
      String apiVersion,
      String phoneNumberId,
      String accessToken,
      Duration timeout,
      int maxTextLength,
      boolean sendingAllowed) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.baseUri = requireEndpoint(baseUri);
    this.apiVersion = required(apiVersion, "WhatsApp API version");
    this.phoneNumberId = required(phoneNumberId, "WhatsApp phone number ID");
    this.accessToken = required(accessToken, "WhatsApp access token");
    this.timeout = timeout;
    this.maxTextLength = maxTextLength;
    this.sendingAllowed = sendingAllowed;
    if (!VERSION.matcher(apiVersion).matches()) {
      throw new IllegalArgumentException("WhatsApp API version must match vN.N");
    }
    if (!DIGITS.matcher(phoneNumberId).matches()) {
      throw new IllegalArgumentException("WhatsApp phone number ID must contain digits only");
    }
  }

  @Override
  public String name() {
    return "WHATSAPP_CLOUD";
  }

  @Override
  public ProviderResult send(OutboundMessage message) {
    if (!sendingAllowed) {
      throw new ProviderException("WhatsApp Cloud adapter is disabled", "SEND_DISABLED", false);
    }
    String recipient = digits(message.recipient());
    if (message.textBody() == null
        || message.textBody().isBlank()
        || message.textBody().length() > maxTextLength) {
      throw new IllegalArgumentException("WhatsApp text length is invalid");
    }
    String body =
        json(
            Map.of(
                "messaging_product",
                "whatsapp",
                "recipient_type",
                "individual",
                "to",
                recipient,
                "type",
                "text",
                "text",
                Map.of("preview_url", false, "body", message.textBody())));
    HttpRequest request =
        HttpRequest.newBuilder(
                baseUri.resolve("/" + apiVersion + "/" + phoneNumberId + "/messages"))
            .timeout(timeout)
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      requireSuccess(response.statusCode());
      JsonNode json = objectMapper.readTree(response.body());
      String id = json.path("messages").path(0).path("id").asText(null);
      if (id == null || id.isBlank()) {
        throw new ProviderException(
            "WhatsApp response omitted message id", "WHATSAPP_INVALID_RESPONSE", false);
      }
      return new ProviderResult("SENT", name(), id, null);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ProviderException("WhatsApp request interrupted", "WHATSAPP_INTERRUPTED", true);
    } catch (java.io.IOException | JacksonException exception) {
      throw new ProviderException("WhatsApp request failed", "WHATSAPP_IO", true);
    }
  }

  private void requireSuccess(int status) {
    if (status >= 200 && status < 300) {
      return;
    }
    throw new ProviderException(
        "WhatsApp returned HTTP " + status,
        "WHATSAPP_HTTP_" + status,
        status == 429 || status >= 500);
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("WhatsApp payload cannot be serialized", exception);
    }
  }

  private String digits(String value) {
    String digits = value == null ? "" : value.replaceAll("\\D", "");
    if (!DIGITS.matcher(digits).matches()) {
      throw new IllegalArgumentException("WhatsApp recipient must be an international number");
    }
    return digits;
  }

  private URI requireEndpoint(URI uri) {
    if (uri == null
        || !("https".equalsIgnoreCase(uri.getScheme())
            || ("http".equalsIgnoreCase(uri.getScheme())
                && Set.of("localhost", "127.0.0.1").contains(uri.getHost())))) {
      throw new IllegalArgumentException("WhatsApp endpoint must use HTTPS or test loopback");
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
