package com.gestudio.crm.messaging;

import java.util.UUID;

public interface MessageDispatcher {
  MessageView createDraft(CreateMessageCommand command);

  MessageView simulate(CreateMessageCommand command);

  ManualLink manualLink(CreateMessageCommand command);

  record CreateMessageCommand(
      UUID prospectId,
      UUID contactId,
      String channel,
      String subject,
      String textBody,
      String htmlBody,
      String idempotencyKey) {}

  record MessageView(
      UUID id,
      String channel,
      String status,
      String sendingBlockReason,
      String provider,
      String externalMessageId,
      String externalThreadId) {}

  record ManualLink(String channel, String status, String url, String sendingBlockReason) {}
}
