package com.gestudio.crm.gmail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class UnsubscribeTokenService {

  private static final byte[] TOKEN_DOMAIN =
      "gestudio:unsubscribe-token:v1".getBytes(StandardCharsets.UTF_8);
  private static final byte[] SUPPRESSION_DOMAIN =
      "gestudio:global-suppression:v1".getBytes(StandardCharsets.UTF_8);

  private final GmailDeliveryProperties properties;

  public UnsubscribeTokenService(GmailDeliveryProperties properties) {
    this.properties = properties;
  }

  public IssuedToken issue(UUID tokenId, UUID organizationId, UUID messageId) {
    String authenticator = authenticator(tokenId, organizationId, messageId);
    String opaqueToken = tokenId + "." + authenticator;
    String hash = sha256(opaqueToken);
    String separator = properties.unsubscribeBaseUri().toString().endsWith("/") ? "" : "/";
    URI uri = URI.create(properties.unsubscribeBaseUri() + separator + opaqueToken);
    return new IssuedToken(tokenId, opaqueToken, hash, properties.unsubscribeSigningKeyId(), uri);
  }

  public boolean verify(
      UUID organizationId, UUID messageId, String opaqueToken, String storedHash, String keyId) {
    if (opaqueToken == null
        || storedHash == null
        || keyId == null
        || !keyId.equals(properties.unsubscribeSigningKeyId())) {
      return false;
    }
    UUID tokenId = tokenId(opaqueToken);
    if (tokenId == null) {
      return false;
    }
    String expectedOpaqueToken = tokenId + "." + authenticator(tokenId, organizationId, messageId);
    byte[] expectedToken = expectedOpaqueToken.getBytes(StandardCharsets.US_ASCII);
    byte[] actualToken = opaqueToken.getBytes(StandardCharsets.US_ASCII);
    byte[] expectedHash = sha256(opaqueToken).getBytes(StandardCharsets.US_ASCII);
    return MessageDigest.isEqual(expectedToken, actualToken)
        && MessageDigest.isEqual(expectedHash, storedHash.getBytes(StandardCharsets.US_ASCII));
  }

  public boolean verify(
      UUID tokenId,
      UUID organizationId,
      UUID messageId,
      String opaqueToken,
      String storedHash,
      String keyId) {
    if (tokenId == null || !tokenId.equals(tokenId(opaqueToken))) {
      return false;
    }
    return verify(organizationId, messageId, opaqueToken, storedHash, keyId);
  }

  public UUID tokenId(String opaqueToken) {
    if (opaqueToken == null || opaqueToken.length() > 128) {
      return null;
    }
    int separator = opaqueToken.indexOf('.');
    if (separator != 36 || opaqueToken.indexOf('.', separator + 1) >= 0) {
      return null;
    }
    try {
      return UUID.fromString(opaqueToken.substring(0, separator));
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  public String suppressionHash(String normalizedValue) {
    if (normalizedValue == null || normalizedValue.isBlank() || normalizedValue.length() > 320) {
      throw new IllegalArgumentException("Normalized suppression value is required");
    }
    return HexFormat.of().formatHex(hmac(SUPPRESSION_DOMAIN, normalizedValue));
  }

  private String authenticator(UUID tokenId, UUID organizationId, UUID messageId) {
    if (tokenId == null || organizationId == null || messageId == null) {
      throw new IllegalArgumentException("Unsubscribe token context is required");
    }
    String context = tokenId + "\n" + organizationId + "\n" + messageId;
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(TOKEN_DOMAIN, context));
  }

  private byte[] hmac(byte[] domain, String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(
          new SecretKeySpec(
              GmailDeliveryProperties.decodeKey(
                  properties.unsubscribeSigningKey(), "unsubscribe signing key"),
              "HmacSHA256"));
      mac.update(domain);
      mac.update((byte) '\n');
      return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("Unsubscribe token signing is unavailable", exception);
    }
  }

  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(StandardCharsets.US_ASCII)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  public record IssuedToken(
      UUID tokenId,
      @JsonIgnore String opaqueToken,
      @JsonIgnore String tokenHash,
      String keyId,
      @JsonIgnore URI publicUri) {
    @Override
    public String toString() {
      return "IssuedToken[tokenId=" + tokenId + ",REDACTED,keyId=" + keyId + "]";
    }
  }
}
