package com.gestudio.crm.gmail;

import com.gestudio.crm.messaging.EmailProvider;
import com.gestudio.crm.messaging.OutboundMessage;
import com.gestudio.crm.messaging.ProviderException;
import com.gestudio.crm.messaging.ProviderResult;

public final class CampaignOnlyGmailEmailProvider implements EmailProvider {

  @Override
  public String name() {
    return "GMAIL";
  }

  @Override
  public ProviderResult createDraft(OutboundMessage message) {
    throw blocked();
  }

  @Override
  public ProviderResult send(OutboundMessage message) {
    throw blocked();
  }

  private ProviderException blocked() {
    return new ProviderException(
        "Gmail live delivery is available only through an approved campaign outbox",
        "GMAIL_CAMPAIGN_OUTBOX_REQUIRED",
        false);
  }
}
