package com.gestudio.crm.messaging;

public class NoopWhatsAppProvider implements WhatsAppProvider {

  @Override
  public String name() {
    return "NOOP";
  }

  @Override
  public ProviderResult send(OutboundMessage message) {
    return new ProviderResult("BLOCKED_BY_CONFIGURATION", name(), null, null);
  }
}
