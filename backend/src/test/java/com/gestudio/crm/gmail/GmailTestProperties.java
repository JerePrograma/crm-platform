package com.gestudio.crm.gmail;

import java.time.Duration;
import java.util.Base64;

final class GmailTestProperties {

  private GmailTestProperties() {}

  static GmailDeliveryProperties properties(String baseUrl) {
    return properties(baseUrl, key((byte) 1), "v1:" + key((byte) 2), "v1", Duration.ofSeconds(2));
  }

  static GmailDeliveryProperties properties(
      String baseUrl, String signingKey, String keyRing, String activeKeyId) {
    return properties(baseUrl, signingKey, keyRing, activeKeyId, Duration.ofSeconds(2));
  }

  static GmailDeliveryProperties properties(String baseUrl, Duration timeout) {
    return properties(baseUrl, key((byte) 1), "v1:" + key((byte) 2), "v1", timeout);
  }

  private static GmailDeliveryProperties properties(
      String baseUrl, String signingKey, String keyRing, String activeKeyId, Duration timeout) {
    return new GmailDeliveryProperties(
        baseUrl + "/auth",
        baseUrl + "/token",
        baseUrl + "/userinfo",
        baseUrl + "/revoke",
        baseUrl,
        "synthetic-client-id",
        "synthetic-client-secret",
        baseUrl + "/oauth/callback",
        baseUrl + "/settings",
        GmailDeliveryProperties.DEFAULT_SCOPES,
        keyRing,
        activeKeyId,
        baseUrl + "/api/v1/unsubscribe",
        signingKey,
        "sign-v1",
        Duration.ofMinutes(10),
        timeout,
        10,
        1,
        baseUrl.startsWith("http://"));
  }

  static String key(byte fill) {
    byte[] bytes = new byte[32];
    java.util.Arrays.fill(bytes, fill);
    return Base64.getEncoder().encodeToString(bytes);
  }
}
