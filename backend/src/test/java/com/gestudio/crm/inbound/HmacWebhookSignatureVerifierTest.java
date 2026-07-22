package com.gestudio.crm.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class HmacWebhookSignatureVerifierTest {

  private static final String SECRET = "synthetic-test-secret";

  @Test
  void acceptsExactSignatureAndRejectsTampering() throws Exception {
    InboundProperties properties =
        new InboundProperties(true, SECRET, Duration.ofMinutes(5), 32768, 60);
    HmacWebhookSignatureVerifier verifier = new HmacWebhookSignatureVerifier(properties);
    UUID organizationId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    byte[] body = "{\"event\":\"synthetic\"}".getBytes(StandardCharsets.UTF_8);
    long timestamp = 1_785_000_000L;
    String nonce = "nonce-1";
    String signature = sign(organizationId, timestamp, nonce, body);

    assertThat(verifier.verify(organizationId, timestamp, nonce, body, signature)).isTrue();
    assertThat(
            verifier.verify(
                organizationId,
                timestamp,
                nonce,
                "{\"event\":\"tampered\"}".getBytes(StandardCharsets.UTF_8),
                signature))
        .isFalse();
    assertThat(verifier.verify(organizationId, timestamp, nonce, body, "not-hex")).isFalse();
  }

  private String sign(UUID organizationId, long timestamp, String nonce, byte[] body)
      throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    mac.update(
        (timestamp + "." + nonce + "." + organizationId + ".").getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(mac.doFinal(body));
  }
}
