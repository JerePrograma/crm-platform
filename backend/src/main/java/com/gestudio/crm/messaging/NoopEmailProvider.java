package com.gestudio.crm.messaging;

public class NoopEmailProvider implements EmailProvider {

  @Override
  public String name() {
    return "NOOP";
  }

  @Override
  public ProviderResult createDraft(OutboundMessage message) {
    return new ProviderResult("BLOCKED_BY_CONFIGURATION", name(), null, null);
  }

  @Override
  public ProviderResult send(OutboundMessage message) {
    throw new ProviderException("Email provider is disabled", "PROVIDER_DISABLED", false);
  }
}
