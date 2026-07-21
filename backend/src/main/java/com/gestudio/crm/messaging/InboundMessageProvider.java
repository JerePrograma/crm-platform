package com.gestudio.crm.messaging;

import java.util.List;

public interface InboundMessageProvider {
  List<InboundEnvelope> readAfter(String cursor, int limit);

  record InboundEnvelope(
      String providerEventId,
      String externalMessageId,
      String externalThreadId,
      String sender,
      String text,
      String nextCursor) {}
}
