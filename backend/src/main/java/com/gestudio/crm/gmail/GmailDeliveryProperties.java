package com.gestudio.crm.gmail;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crm.gmail")
public record GmailDeliveryProperties(
    String authorizationUrl,
    String tokenUrl,
    String userinfoUrl,
    String revocationUrl,
    String apiBaseUrl,
    String clientId,
    String clientSecret,
    String redirectUri,
    String frontendReturnUri,
    String scopes,
    String tokenEncryptionKeys,
    String activeEncryptionKeyId,
    String unsubscribePublicBaseUrl,
    String unsubscribeSigningKey,
    String unsubscribeSigningKeyId,
    Duration stateTtl,
    Duration httpTimeout,
    int hardDailyLimit,
    int maxConcurrency,
    boolean allowTestEndpoints) {

  public static final String SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send";
  public static final String DEFAULT_SCOPES = "openid email " + SEND_SCOPE;

  public GmailDeliveryProperties {
    authorizationUrl =
        defaultValue(authorizationUrl, "https://accounts.google.com/o/oauth2/v2/auth");
    tokenUrl = defaultValue(tokenUrl, "https://oauth2.googleapis.com/token");
    userinfoUrl = defaultValue(userinfoUrl, "https://www.googleapis.com/oauth2/v3/userinfo");
    revocationUrl = defaultValue(revocationUrl, "https://oauth2.googleapis.com/revoke");
    apiBaseUrl = defaultValue(apiBaseUrl, "https://gmail.googleapis.com");
    scopes = defaultValue(scopes, DEFAULT_SCOPES);
    stateTtl = stateTtl == null ? Duration.ofMinutes(10) : stateTtl;
    httpTimeout = httpTimeout == null ? Duration.ofSeconds(10) : httpTimeout;
    hardDailyLimit = hardDailyLimit == 0 ? 10 : hardDailyLimit;
    maxConcurrency = maxConcurrency == 0 ? 1 : maxConcurrency;
    if (stateTtl.isNegative() || stateTtl.isZero() || stateTtl.compareTo(Duration.ofHours(1)) > 0) {
      throw new IllegalArgumentException(
          "Gmail OAuth state TTL must be between 1 second and 1 hour");
    }
    if (httpTimeout.isNegative()
        || httpTimeout.isZero()
        || httpTimeout.compareTo(Duration.ofMinutes(2)) > 0) {
      throw new IllegalArgumentException(
          "Gmail HTTP timeout must be between 1 second and 2 minutes");
    }
    if (hardDailyLimit < 1 || hardDailyLimit > 10000) {
      throw new IllegalArgumentException("Gmail hard daily limit must be between 1 and 10000");
    }
    if (maxConcurrency != 1) {
      throw new IllegalArgumentException("Gmail live concurrency must remain 1");
    }
  }

  public boolean oauthConfigured() {
    return present(clientId)
        && present(clientSecret)
        && present(redirectUri)
        && present(frontendReturnUri)
        && present(tokenEncryptionKeys)
        && present(activeEncryptionKeyId);
  }

  public Set<String> scopeSet() {
    return Arrays.stream(scopes.split("[ ,]+"))
        .map(String::trim)
        .filter(item -> !item.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
  }

  public void requireLiveConfigured() {
    if (!oauthConfigured()
        || !present(unsubscribePublicBaseUrl)
        || !present(unsubscribeSigningKey)
        || !present(unsubscribeSigningKeyId)) {
      throw new IllegalStateException(
          "Gmail live mode requires complete OAuth and key configuration");
    }
    if (!scopeSet().contains(SEND_SCOPE) || !scopeSet().contains("email")) {
      throw new IllegalStateException("Gmail live mode requires gmail.send and email scopes");
    }
    endpoint(authorizationUrl, "authorization URL");
    endpoint(tokenUrl, "token URL");
    endpoint(userinfoUrl, "userinfo URL");
    endpoint(revocationUrl, "revocation URL");
    endpoint(apiBaseUrl, "API base URL");
    exactUri(redirectUri, "redirect URI");
    exactUri(frontendReturnUri, "frontend return URI");
    exactUri(unsubscribePublicBaseUrl, "unsubscribe public base URL");
    GmailTokenCipher.validateKeyRing(tokenEncryptionKeys, activeEncryptionKeyId);
    decodeKey(unsubscribeSigningKey, "unsubscribe signing key");
  }

  public URI authorizationUri() {
    return endpoint(authorizationUrl, "authorization URL");
  }

  public URI tokenUri() {
    return endpoint(tokenUrl, "token URL");
  }

  public URI userinfoUri() {
    return endpoint(userinfoUrl, "userinfo URL");
  }

  public URI revocationUri() {
    return endpoint(revocationUrl, "revocation URL");
  }

  public URI gmailApiBaseUri() {
    return endpoint(apiBaseUrl, "API base URL");
  }

  public URI callbackUri() {
    return exactUri(redirectUri, "redirect URI");
  }

  public URI frontendReturn() {
    return exactUri(frontendReturnUri, "frontend return URI");
  }

  public URI unsubscribeBaseUri() {
    return exactUri(unsubscribePublicBaseUrl, "unsubscribe public base URL");
  }

  public boolean isApprovedUnsubscribeUri(URI candidate) {
    if (candidate == null) {
      return false;
    }
    URI base = unsubscribeBaseUri();
    String basePath = base.getPath().endsWith("/") ? base.getPath() : base.getPath() + "/";
    return base.getScheme().equalsIgnoreCase(candidate.getScheme())
        && base.getHost().equalsIgnoreCase(candidate.getHost())
        && effectivePort(base) == effectivePort(candidate)
        && candidate.getUserInfo() == null
        && candidate.getFragment() == null
        && candidate.getPath().startsWith(basePath);
  }

  static byte[] decodeKey(String encoded, String label) {
    try {
      byte[] key = java.util.Base64.getDecoder().decode(encoded == null ? "" : encoded.trim());
      if (key.length != 32) {
        throw new IllegalArgumentException(label + " must contain exactly 32 bytes");
      }
      return key;
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(label + " must be base64-encoded 32-byte material");
    }
  }

  private URI endpoint(String value, String label) {
    URI uri = parse(value, label);
    boolean https = "https".equalsIgnoreCase(uri.getScheme());
    boolean allowedTest =
        allowTestEndpoints
            && "http".equalsIgnoreCase(uri.getScheme())
            && Set.of("localhost", "127.0.0.1", "fake-google", "host.docker.internal")
                .contains(uri.getHost().toLowerCase(Locale.ROOT));
    if ((!https && !allowedTest) || uri.getUserInfo() != null || uri.getFragment() != null) {
      throw new IllegalArgumentException(label + " must use an approved HTTPS endpoint");
    }
    return uri;
  }

  private URI exactUri(String value, String label) {
    URI uri = endpoint(value, label);
    if (uri.getQuery() != null) {
      throw new IllegalArgumentException(label + " must not contain a query string");
    }
    return uri;
  }

  private URI parse(String value, String label) {
    if (!present(value)) {
      throw new IllegalArgumentException(label + " is required");
    }
    URI uri = URI.create(value.trim());
    if (uri.getHost() == null) {
      throw new IllegalArgumentException(label + " must be absolute");
    }
    return uri;
  }

  private static String defaultValue(String value, String fallback) {
    return present(value) ? value.trim() : fallback;
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }

  private int effectivePort(URI uri) {
    if (uri.getPort() >= 0) {
      return uri.getPort();
    }
    return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
  }
}
