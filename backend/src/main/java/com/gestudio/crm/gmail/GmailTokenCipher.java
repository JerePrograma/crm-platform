package com.gestudio.crm.gmail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GmailTokenCipher {

  private static final int NONCE_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final byte[] AAD =
      "gestudio:gmail-refresh-token:v1".getBytes(StandardCharsets.UTF_8);

  private final GmailDeliveryProperties properties;
  private final SecureRandom secureRandom;

  @Autowired
  public GmailTokenCipher(GmailDeliveryProperties properties) {
    this(properties, new SecureRandom());
  }

  GmailTokenCipher(GmailDeliveryProperties properties, SecureRandom secureRandom) {
    this.properties = properties;
    this.secureRandom = secureRandom;
  }

  public EncryptedSecret encrypt(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Refresh token is required");
    }
    Map<String, byte[]> keys = keyRing();
    String keyId = properties.activeEncryptionKeyId().trim();
    byte[] nonce = new byte[NONCE_BYTES];
    secureRandom.nextBytes(nonce);
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(keys.get(keyId), "AES"),
          new GCMParameterSpec(TAG_BITS, nonce));
      cipher.updateAAD(AAD);
      return new EncryptedSecret(
          cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), nonce, keyId);
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Refresh token encryption is unavailable", exception);
    }
  }

  public String decrypt(EncryptedSecret secret) {
    if (secret == null
        || secret.keyId() == null
        || secret.ciphertext() == null
        || secret.nonce() == null) {
      throw new IllegalArgumentException("Encrypted refresh token is incomplete");
    }
    byte[] key = keyRing().get(secret.keyId());
    if (key == null) {
      throw new IllegalArgumentException("Encrypted refresh token uses an unknown key version");
    }
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key, "AES"),
          new GCMParameterSpec(TAG_BITS, secret.nonce()));
      cipher.updateAAD(AAD);
      return new String(cipher.doFinal(secret.ciphertext()), StandardCharsets.UTF_8);
    } catch (AEADBadTagException exception) {
      throw new IllegalArgumentException("Encrypted refresh token failed integrity validation");
    } catch (GeneralSecurityException exception) {
      throw new IllegalArgumentException("Encrypted refresh token cannot be decrypted");
    }
  }

  public EncryptedSecret rotate(EncryptedSecret secret) {
    if (properties.activeEncryptionKeyId().equals(secret.keyId())) {
      return secret;
    }
    return encrypt(decrypt(secret));
  }

  static void validateKeyRing(String specification, String activeKeyId) {
    Map<String, byte[]> keys = parseKeyRing(specification);
    if (activeKeyId == null || activeKeyId.isBlank() || !keys.containsKey(activeKeyId.trim())) {
      throw new IllegalArgumentException(
          "Active Gmail encryption key ID is missing from the keyring");
    }
  }

  private Map<String, byte[]> keyRing() {
    validateKeyRing(properties.tokenEncryptionKeys(), properties.activeEncryptionKeyId());
    return parseKeyRing(properties.tokenEncryptionKeys());
  }

  private static Map<String, byte[]> parseKeyRing(String specification) {
    Map<String, byte[]> keys = new LinkedHashMap<>();
    if (specification != null) {
      for (String entry : specification.split(",")) {
        String[] pair = entry.trim().split(":", 2);
        if (pair.length != 2 || !pair[0].matches("[A-Za-z0-9._-]{1,64}")) {
          throw new IllegalArgumentException("Gmail encryption keyring entry is invalid");
        }
        if (keys.putIfAbsent(
                pair[0], GmailDeliveryProperties.decodeKey(pair[1], "Gmail encryption key"))
            != null) {
          throw new IllegalArgumentException(
              "Gmail encryption keyring contains a duplicate key ID");
        }
      }
    }
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("Gmail encryption keyring is empty");
    }
    return Map.copyOf(keys);
  }

  public static final class EncryptedSecret {
    private final byte[] ciphertext;
    private final byte[] nonce;
    private final String keyId;

    public EncryptedSecret(byte[] ciphertext, byte[] nonce, String keyId) {
      this.ciphertext = Arrays.copyOf(ciphertext, ciphertext.length);
      this.nonce = Arrays.copyOf(nonce, nonce.length);
      this.keyId = keyId;
    }

    @JsonIgnore
    public byte[] ciphertext() {
      return Arrays.copyOf(ciphertext, ciphertext.length);
    }

    @JsonIgnore
    public byte[] nonce() {
      return Arrays.copyOf(nonce, nonce.length);
    }

    public String keyId() {
      return keyId;
    }

    @Override
    public String toString() {
      return "EncryptedSecret[REDACTED,keyId=" + keyId + "]";
    }
  }
}
