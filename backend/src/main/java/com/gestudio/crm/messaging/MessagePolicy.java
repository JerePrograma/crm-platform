package com.gestudio.crm.messaging;

import java.util.UUID;

public interface MessagePolicy {
  PolicyDecision evaluate(UUID prospectId, UUID contactId, String channel, boolean realSend);

  record PolicyDecision(
      boolean messageAllowed,
      boolean realSendAllowed,
      String result,
      UUID contactChannelId,
      String recipient) {}
}
