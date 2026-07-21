package com.gestudio.crm.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class FakeEmailProvider implements EmailProvider {

  @Override
  public String name() {
    return "FAKE";
  }

  @Override
  public ProviderResult createDraft(OutboundMessage message) {
    String id =
        "fake-email-"
            + UUID.nameUUIDFromBytes(message.idempotencyKey().getBytes(StandardCharsets.UTF_8));
    return new ProviderResult("DRAFT_CREATED", name(), id, "fake-thread-" + message.messageId());
  }

  @Override
  public ProviderResult send(OutboundMessage message) {
    throw new ProviderException("Fake email never reports SENT", "FAKE_SEND_BLOCKED", false);
  }
}
