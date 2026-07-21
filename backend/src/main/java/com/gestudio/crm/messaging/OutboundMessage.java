package com.gestudio.crm.messaging;

import java.util.UUID;

public record OutboundMessage(
    UUID organizationId,
    UUID messageId,
    String recipient,
    String subject,
    String textBody,
    String htmlBody,
    String idempotencyKey) {}
