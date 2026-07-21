package com.gestudio.crm.messaging;

public interface WhatsAppProvider {
  String name();

  ProviderResult send(OutboundMessage message);
}
