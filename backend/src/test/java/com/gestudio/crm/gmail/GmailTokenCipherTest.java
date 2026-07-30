package com.gestudio.crm.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GmailTokenCipherTest {

  @Test
  void encryptsWithUniqueNonceAndDecrypts() {
    GmailTokenCipher cipher =
        new GmailTokenCipher(GmailTestProperties.properties("https://example.test"));

    var first = cipher.encrypt("synthetic-refresh-token");
    var second = cipher.encrypt("synthetic-refresh-token");

    assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
    assertThat(first.nonce()).isNotEqualTo(second.nonce());
    assertThat(cipher.decrypt(first)).isEqualTo("synthetic-refresh-token");
    assertThat(first.toString()).doesNotContain("synthetic-refresh-token").contains("REDACTED");
  }

  @Test
  void rejectsTamperingAndWrongKey() {
    GmailTokenCipher cipher =
        new GmailTokenCipher(GmailTestProperties.properties("https://example.test"));
    var encrypted = cipher.encrypt("synthetic-refresh-token");
    byte[] changed = encrypted.ciphertext();
    changed[0] ^= 1;

    assertThatThrownBy(
            () ->
                cipher.decrypt(
                    new GmailTokenCipher.EncryptedSecret(
                        changed, encrypted.nonce(), encrypted.keyId())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageNotContaining("synthetic-refresh-token");

    GmailTokenCipher wrong =
        new GmailTokenCipher(
            GmailTestProperties.properties(
                "https://example.test",
                GmailTestProperties.key((byte) 1),
                "v1:" + GmailTestProperties.key((byte) 9),
                "v1"));
    assertThatThrownBy(() -> wrong.decrypt(encrypted))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("integrity");
  }

  @Test
  void rotatesToActiveKeyAndDoesNotSerializeSecretBytes() throws Exception {
    GmailTokenCipher first =
        new GmailTokenCipher(
            GmailTestProperties.properties(
                "https://example.test",
                GmailTestProperties.key((byte) 1),
                "v1:" + GmailTestProperties.key((byte) 2),
                "v1"));
    var encrypted = first.encrypt("synthetic-refresh-token");
    GmailTokenCipher rotatedCipher =
        new GmailTokenCipher(
            GmailTestProperties.properties(
                "https://example.test",
                GmailTestProperties.key((byte) 1),
                "v1:"
                    + GmailTestProperties.key((byte) 2)
                    + ",v2:"
                    + GmailTestProperties.key((byte) 3),
                "v2"));

    var rotated = rotatedCipher.rotate(encrypted);

    assertThat(rotated.keyId()).isEqualTo("v2");
    assertThat(rotatedCipher.decrypt(rotated)).isEqualTo("synthetic-refresh-token");
    String json = new ObjectMapper().writeValueAsString(rotated);
    assertThat(json)
        .doesNotContain("ciphertext", "nonce")
        .doesNotContain(Arrays.toString(rotated.ciphertext()));
  }
}
