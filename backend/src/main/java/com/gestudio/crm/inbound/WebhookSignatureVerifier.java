package com.gestudio.crm.inbound;

import java.util.UUID;

public interface WebhookSignatureVerifier {
  boolean verify(
      UUID organizationId, long timestamp, String nonce, byte[] payload, String providedSignature);
}
