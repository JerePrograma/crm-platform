package com.gestudio.crm.messaging;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.messaging.MessagePolicy.PolicyDecision;
import com.gestudio.crm.security.CurrentActor;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageDispatcherService implements MessageDispatcher {

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final AuditEventWriter auditEventWriter;
  private final MessagePolicy messagePolicy;
  private final MessageRenderer messageRenderer;
  private final EmailProvider emailProvider;
  private final WhatsAppProvider whatsAppProvider;
  private final MessagingProperties properties;

  public MessageDispatcherService(
      JdbcTemplate jdbcTemplate,
      CurrentActor currentActor,
      AuditEventWriter auditEventWriter,
      MessagePolicy messagePolicy,
      MessageRenderer messageRenderer,
      EmailProvider emailProvider,
      WhatsAppProvider whatsAppProvider,
      MessagingProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.auditEventWriter = auditEventWriter;
    this.messagePolicy = messagePolicy;
    this.messageRenderer = messageRenderer;
    this.emailProvider = emailProvider;
    this.whatsAppProvider = whatsAppProvider;
    this.properties = properties;
  }

  @Override
  @Transactional
  public MessageView createDraft(CreateMessageCommand command) {
    MessageView existing = existing(command.idempotencyKey());
    if (existing != null) {
      return existing;
    }
    Prepared prepared = prepare(command);
    ProviderResult provider =
        "EMAIL".equals(prepared.channel())
            ? emailProvider.createDraft(prepared.outbound())
            : new ProviderResult("DRAFT_CREATED", "MANUAL", null, null);
    String status =
        "PROVIDER_DRAFT_CREATED".equals(provider.result())
            ? "PROVIDER_DRAFT_CREATED"
            : "DRAFT_CREATED";
    MessageView message = persist(prepared, status, provider);
    attempt(
        message.id(),
        provider.provider(),
        "CREATE_DRAFT",
        provider.result().startsWith("BLOCKED") ? "BLOCKED" : "SUCCESS",
        provider.externalMessageId(),
        null);
    audit(message, "MESSAGE_DRAFT_CREATED");
    return message;
  }

  @Override
  @Transactional
  public MessageView simulate(CreateMessageCommand command) {
    MessageView existing = existing(command.idempotencyKey());
    if (existing != null) {
      return existing;
    }
    Prepared prepared = prepare(command);
    ProviderResult provider =
        "EMAIL".equals(prepared.channel())
            ? new FakeEmailProvider().createDraft(prepared.outbound())
            : new FakeWhatsAppProvider().send(prepared.outbound());
    MessageView message = persist(prepared, "SIMULATED", provider);
    attempt(
        message.id(),
        provider.provider(),
        "SIMULATE",
        "SUCCESS",
        provider.externalMessageId(),
        null);
    recordActivity(message, command.prospectId(), command.contactId());
    audit(message, "MESSAGE_SIMULATED");
    return message;
  }

  @Override
  @Transactional(readOnly = true)
  public ManualLink manualLink(CreateMessageCommand command) {
    Prepared prepared = prepare(command);
    String encodedBody = URLEncoder.encode(prepared.outbound().textBody(), StandardCharsets.UTF_8);
    String url;
    if ("EMAIL".equals(prepared.channel())) {
      String encodedSubject =
          URLEncoder.encode(prepared.outbound().subject(), StandardCharsets.UTF_8);
      url =
          "mailto:"
              + prepared.outbound().recipient()
              + "?subject="
              + encodedSubject
              + "&body="
              + encodedBody;
    } else {
      String digits = prepared.outbound().recipient().replaceAll("\\D", "");
      url = "https://wa.me/" + digits + "?text=" + encodedBody;
    }
    return new ManualLink(prepared.channel(), "DRAFT_CREATED", url, prepared.policy().result());
  }

  @Transactional(readOnly = true)
  public SafetyView safety() {
    return new SafetyView(
        properties.emailMode(),
        properties.whatsappMode(),
        properties.realNetworkAllowed(),
        emailProvider.name(),
        whatsAppProvider.name(),
        false);
  }

  private Prepared prepare(CreateMessageCommand command) {
    String idempotencyKey = required(command.idempotencyKey(), "Idempotency key");
    String channel = required(command.channel(), "Channel").toUpperCase(java.util.Locale.ROOT);
    if (!Set.of("EMAIL", "WHATSAPP").contains(channel)) {
      throw new IllegalArgumentException("Channel must be EMAIL or WHATSAPP");
    }
    if ("EMAIL".equals(channel) && (command.subject() == null || command.subject().isBlank())) {
      throw new IllegalArgumentException("Email subject is required");
    }
    PolicyDecision policy =
        messagePolicy.evaluate(command.prospectId(), command.contactId(), channel, false);
    if (!policy.messageAllowed()) {
      throw new OptimisticConflictException(policy.result());
    }
    var rendered =
        messageRenderer.render(
            command.subject() == null ? "" : command.subject(),
            required(command.textBody(), "Text body"),
            command.htmlBody() == null ? "" : command.htmlBody(),
            Map.of());
    UUID messageId = UUID.randomUUID();
    OutboundMessage outbound =
        new OutboundMessage(
            currentActor.organizationId(),
            messageId,
            policy.recipient(),
            rendered.subject(),
            rendered.textBody(),
            rendered.htmlBody(),
            idempotencyKey);
    return new Prepared(messageId, command, channel, policy, outbound, policy.contactChannelId());
  }

  private MessageView persist(Prepared prepared, String status, ProviderResult provider) {
    jdbcTemplate.update(
        """
        INSERT INTO message_record (
          id, version, organization_id, prospect_id, contact_id, contact_channel_id,
          created_by, channel, direction, status, sending_block_reason, subject,
          body_text, body_html, provider, external_message_id, external_thread_id,
          idempotency_key, created_at, updated_at
        ) VALUES (?, 0, ?, ?, ?, ?, ?, ?, 'OUTBOUND', ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
        """,
        prepared.messageId(),
        currentActor.organizationId(),
        prepared.command().prospectId(),
        prepared.command().contactId(),
        prepared.channelId(),
        currentActor.userIdOrNull(),
        prepared.channel(),
        status,
        prepared.policy().result(),
        blankToNull(prepared.outbound().subject()),
        prepared.outbound().textBody(),
        blankToNull(prepared.outbound().htmlBody()),
        provider.provider(),
        provider.externalMessageId(),
        provider.externalThreadId(),
        prepared.outbound().idempotencyKey());
    return get(prepared.messageId());
  }

  private void attempt(
      UUID messageId,
      String provider,
      String operation,
      String result,
      String externalId,
      String errorCode) {
    jdbcTemplate.update(
        """
        INSERT INTO message_provider_attempt (
          id, organization_id, message_id, provider, operation, result,
          error_code, external_id, started_at, completed_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
        """,
        UUID.randomUUID(),
        currentActor.organizationId(),
        messageId,
        provider,
        operation,
        result,
        errorCode,
        externalId);
  }

  private void recordActivity(MessageView message, UUID prospectId, UUID contactId) {
    String activityType = "EMAIL".equals(message.channel()) ? "EMAIL_DRAFTED" : "WHATSAPP_DRAFTED";
    jdbcTemplate.update(
        """
        INSERT INTO activity (
          id, organization_id, prospect_id, contact_id, actor_user_id, activity_type,
          occurred_at, channel, direction, outcome, summary, detail,
          external_reference, metadata, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, now(), ?, 'OUTBOUND', 'SIMULATED',
          'Message simulated', 'Fake provider; no network dispatch', ?, '{}'::jsonb, now())
        """,
        UUID.randomUUID(),
        currentActor.organizationId(),
        prospectId,
        contactId,
        currentActor.userIdOrNull(),
        activityType,
        message.channel(),
        "message-simulation:" + message.id());
  }

  private void audit(MessageView message, String action) {
    auditEventWriter.record(
        action,
        "MESSAGE",
        message.id(),
        Map.of(
            "channel",
            message.channel(),
            "status",
            message.status(),
            "provider",
            message.provider(),
            "sendingBlockReason",
            message.sendingBlockReason()));
  }

  private MessageView existing(String key) {
    if (key == null || key.isBlank()) {
      return null;
    }
    return jdbcTemplate
        .query(
            select() + " WHERE organization_id = ? AND idempotency_key = ?",
            this::view,
            currentActor.organizationId(),
            key.trim())
        .stream()
        .findFirst()
        .orElse(null);
  }

  private MessageView get(UUID id) {
    return jdbcTemplate
        .query(
            select() + " WHERE organization_id = ? AND id = ?",
            this::view,
            currentActor.organizationId(),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + id));
  }

  private String select() {
    return """
        SELECT id, channel, status, sending_block_reason, provider,
          external_message_id, external_thread_id FROM message_record
        """;
  }

  private MessageView view(ResultSet rs, int rowNum) throws SQLException {
    return new MessageView(
        rs.getObject("id", UUID.class),
        rs.getString("channel"),
        rs.getString("status"),
        rs.getString("sending_block_reason"),
        rs.getString("provider"),
        rs.getString("external_message_id"),
        rs.getString("external_thread_id"));
  }

  private String required(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " is required");
    }
    return value.trim();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private record Prepared(
      UUID messageId,
      CreateMessageCommand command,
      String channel,
      PolicyDecision policy,
      OutboundMessage outbound,
      UUID channelId) {}

  public record SafetyView(
      String emailMode,
      String whatsAppMode,
      boolean realNetworkAllowed,
      String selectedEmailProvider,
      String selectedWhatsAppProvider,
      boolean sendEndpointAvailable) {}
}
