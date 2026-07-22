package com.gestudio.crm.inbound;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacWebhookSignatureVerifier implements WebhookSignatureVerifier {

  private final InboundProperties properties;

  public HmacWebhookSignatureVerifier(InboundProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean verify(
      UUID organizationId, long timestamp, String nonce, byte[] payload, String providedSignature) {
    if (!properties.configured()
        || organizationId == null
        || nonce == null
        || nonce.isBlank()
        || providedSignature == null) {
      return false;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(
          new SecretKeySpec(
              properties.fakeWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      mac.update(
          (timestamp + "." + nonce + "." + organizationId + ".").getBytes(StandardCharsets.UTF_8));
      byte[] expected = mac.doFinal(payload);
      byte[] provided = HexFormat.of().parseHex(providedSignature);
      return MessageDigest.isEqual(expected, provided);
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      return false;
    }
  }
}
