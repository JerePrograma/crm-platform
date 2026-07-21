package com.gestudio.crm.messaging;

public interface EmailProvider {
  String name();

  ProviderResult createDraft(OutboundMessage message);

  ProviderResult send(OutboundMessage message);
}
