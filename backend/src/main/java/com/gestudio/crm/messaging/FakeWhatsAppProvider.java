package com.gestudio.crm.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class FakeWhatsAppProvider implements WhatsAppProvider {

  @Override
  public String name() {
    return "FAKE";
  }

  @Override
  public ProviderResult send(OutboundMessage message) {
    String id =
        "fake-whatsapp-"
            + UUID.nameUUIDFromBytes(message.idempotencyKey().getBytes(StandardCharsets.UTF_8));
    return new ProviderResult("SIMULATED", name(), id, null);
  }
}
