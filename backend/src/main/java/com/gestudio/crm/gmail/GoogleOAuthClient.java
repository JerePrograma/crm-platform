package com.gestudio.crm.gmail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class GoogleOAuthClient {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final GmailDeliveryProperties properties;
  private final Clock clock;

  public GoogleOAuthClient(
      HttpClient httpClient,
      ObjectMapper objectMapper,
      GmailDeliveryProperties properties,
      Clock clock) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.clock = clock;
  }

  public URI authorizationUri(String state) {
    if (state == null || !state.matches("[A-Za-z0-9_-]{40,128}")) {
      throw new IllegalArgumentException("OAuth state is invalid");
    }
    Map<String, String> query = new LinkedHashMap<>();
    query.put("client_id", required(properties.clientId(), "OAuth client ID"));
    query.put("redirect_uri", properties.callbackUri().toASCIIString());
    query.put("response_type", "code");
    query.put("scope", String.join(" ", properties.scopeSet()));
    query.put("access_type", "offline");
    query.put("include_granted_scopes", "true");
    query.put("prompt", "consent");
    query.put("state", state);
    return URI.create(properties.authorizationUri() + "?" + form(query));
  }

  public OAuthTokens exchangeAuthorizationCode(String code) {
    if (code == null || code.isBlank() || code.length() > 4096 || containsLineBreak(code)) {
      throw new GoogleOAuthException(
          GoogleOAuthException.Code.INVALID_RESPONSE, "Google authorization code is invalid");
    }
    return token(
        Map.of(
            "client_id", required(properties.clientId(), "OAuth client ID"),
            "client_secret", required(properties.clientSecret(), "OAuth client secret"),
            "code", code,
            "grant_type", "authorization_code",
            "redirect_uri", properties.callbackUri().toASCIIString()));
  }

  public OAuthTokens refreshAccessToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank() || refreshToken.length() > 8192) {
      throw new GoogleOAuthException(
          GoogleOAuthException.Code.INVALID_GRANT, "Gmail authorization must be renewed");
    }
    return token(
        Map.of(
            "client_id",
            required(properties.clientId(), "OAuth client ID"),
            "client_secret",
            required(properties.clientSecret(), "OAuth client secret"),
            "refresh_token",
            refreshToken,
            "grant_type",
            "refresh_token"));
  }

  public UserInfo userInfo(String accessToken) {
    JsonNode json = get(properties.userinfoUri(), accessToken);
    String email = text(json, "email", 320);
    boolean verified = json.path("email_verified").asBoolean(false);
    if (!verified) {
      throw new GoogleOAuthException(
          GoogleOAuthException.Code.INVALID_RESPONSE, "Google account email is not verified");
    }
    return new UserInfo(email, json.path("name").asText(null), true);
  }

  public void revoke(String refreshToken) {
    HttpResponse<String> response =
        post(properties.revocationUri(), Map.of("token", refreshToken), "OAuth revocation");
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new GoogleOAuthException(
          GoogleOAuthException.Code.REMOTE_FAILURE,
          "Google authorization could not be revoked remotely");
    }
  }

  private OAuthTokens token(Map<String, String> fields) {
    HttpResponse<String> response = post(properties.tokenUri(), fields, "OAuth token");
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw oauthFailure(response);
    }
    try {
      JsonNode json = objectMapper.readTree(response.body());
      String accessToken = text(json, "access_token", 8192);
      long expiresIn = json.path("expires_in").asLong(3600);
      if (expiresIn < 1 || expiresIn > 86_400) {
        throw new IllegalArgumentException("Invalid token lifetime");
      }
      Set<String> scopes = scopes(json.path("scope").asText(properties.scopes()));
      return new OAuthTokens(
          accessToken,
          json.path("refresh_token").asText(null),
          clock.instant().plusSeconds(expiresIn),
          scopes);
    } catch (JacksonException | IllegalArgumentException exception) {
      throw new GoogleOAuthException(
          GoogleOAuthException.Code.INVALID_RESPONSE, "Google returned an invalid token response");
    }
  }

  private HttpResponse<String> post(URI uri, Map<String, String> fields, String operation) {
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(properties.httpTimeout())
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form(fields), StandardCharsets.UTF_8))
            .build();
    return send(request, operation);
  }

  private JsonNode get(URI uri, String accessToken) {
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(properties.httpTimeout())
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .build();
    HttpResponse<String> response = send(request, "OAuth userinfo");
    if (response.statusCode() == 401) {
      throw new GoogleOAuthException(
          GoogleOAuthException.Code.INVALID_GRANT, "Gmail authorization must be renewed");
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new GoogleOAuthException(
          GoogleOAuthException.Code.REMOTE_FAILURE, "Google account verification failed");
    }
    try {
      return objectMapper.readTree(response.body());
    } catch (JacksonException exception) {
      throw new GoogleOAuthException(
          GoogleOAuthException.Code.INVALID_RESPONSE,
          "Google returned invalid account information");
    }
  }

  private HttpResponse<String> send(HttpRequest request, String operation) {
    try {
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new GoogleOAuthException(
          GoogleOAuthException.Code.AMBIGUOUS, operation + " was interrupted");
    } catch (IOException exception) {
      throw new GoogleOAuthException(
          GoogleOAuthException.Code.REMOTE_FAILURE, operation + " request failed");
    }
  }

  private GoogleOAuthException oauthFailure(HttpResponse<String> response) {
    String error = null;
    if (response.body() != null && response.body().length() <= 64_000) {
      try {
        error = objectMapper.readTree(response.body()).path("error").asText(null);
      } catch (JacksonException ignored) {
        // The remote response is deliberately not propagated.
      }
    }
    if ("invalid_grant".equals(error)) {
      return new GoogleOAuthException(
          GoogleOAuthException.Code.INVALID_GRANT, "Gmail authorization must be renewed");
    }
    if ("invalid_scope".equals(error)) {
      return new GoogleOAuthException(
          GoogleOAuthException.Code.INSUFFICIENT_SCOPE, "Google did not grant the required scope");
    }
    return new GoogleOAuthException(
        GoogleOAuthException.Code.REMOTE_FAILURE, "Google rejected the OAuth request");
  }

  private Set<String> scopes(String value) {
    return Arrays.stream(value == null ? new String[0] : value.split("[ ,]+"))
        .map(String::trim)
        .filter(item -> !item.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
  }

  private String text(JsonNode json, String field, int maximumLength) {
    String value = json.path(field).asText(null);
    if (value == null || value.isBlank() || value.length() > maximumLength) {
      throw new IllegalArgumentException("Google response omitted " + field);
    }
    return value;
  }

  private String form(Map<String, String> fields) {
    return fields.entrySet().stream()
        .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
        .collect(Collectors.joining("&"));
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private String required(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(label + " is not configured");
    }
    return value.trim();
  }

  private boolean containsLineBreak(String value) {
    return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
  }

  public static final class OAuthTokens {
    private final String accessToken;
    private final String refreshToken;
    private final Instant expiresAt;
    private final Set<String> scopes;

    OAuthTokens(String accessToken, String refreshToken, Instant expiresAt, Set<String> scopes) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      this.expiresAt = expiresAt;
      this.scopes = Set.copyOf(scopes);
    }

    @JsonIgnore
    public String accessToken() {
      return accessToken;
    }

    @JsonIgnore
    public String refreshToken() {
      return refreshToken;
    }

    public Instant expiresAt() {
      return expiresAt;
    }

    public Set<String> scopes() {
      return scopes;
    }

    @Override
    public String toString() {
      return "OAuthTokens[REDACTED,expiresAt=" + expiresAt + ",scopes=" + scopes + "]";
    }
  }

  public record UserInfo(String email, String displayName, boolean emailVerified) {}
}
