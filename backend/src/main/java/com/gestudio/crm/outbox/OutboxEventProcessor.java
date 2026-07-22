package com.gestudio.crm.outbox;

import com.gestudio.crm.inbound.InboundProcessingService;
import com.gestudio.crm.settings.SendingProperties;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxEventProcessor {

  private final InboundProcessingService inboundProcessingService;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final SendingProperties sendingProperties;

  public OutboxEventProcessor(
      InboundProcessingService inboundProcessingService,
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      SendingProperties sendingProperties) {
    this.inboundProcessingService = inboundProcessingService;
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.sendingProperties = sendingProperties;
  }

  public OutboxProcessingResult process(OutboxWorkerService.ClaimedEvent event) {
    return switch (event.eventType()) {
      case "INBOUND_RECEIVED_V1" -> {
        inboundProcessingService.process(
            event.organizationId(), requiredUuid(event, "inboundMessageId"), event.correlationId());
        yield OutboxProcessingResult.success("INBOUND_PROCESSED");
      }
      case "CAMPAIGN_SIMULATED_V1", "MESSAGE_RESULT_CREATED_V1" -> safeMessagingResult(event);
      default ->
          OutboxProcessingResult.failure(
              OutboxErrorCategory.NON_RETRYABLE,
              "UNSUPPORTED_EVENT",
              "Unsupported outbox event type");
    };
  }

  private OutboxProcessingResult safeMessagingResult(OutboxWorkerService.ClaimedEvent event) {
    if (event.eventType().equals("MESSAGE_RESULT_CREATED_V1")) {
      OutboxProcessingResult currentPolicy = currentMessagePolicy(event);
      if (currentPolicy != null) {
        return currentPolicy;
      }
    }
    Map<String, String> settings =
        jdbcTemplate.query(
            """
            SELECT setting_key, setting_value FROM system_setting
            WHERE organization_id = ? AND setting_key IN
              ('sending.enabled', 'sending.dry-run', 'sending.daily-limit', 'sending.kill-switch')
            """,
            rs -> {
              Map<String, String> values = new java.util.HashMap<>();
              while (rs.next()) {
                values.put(rs.getString(1), rs.getString(2));
              }
              return values;
            },
            event.organizationId());
    boolean databaseBlocked =
        !Boolean.parseBoolean(settings.getOrDefault("sending.enabled", "false"))
            || Boolean.parseBoolean(settings.getOrDefault("sending.dry-run", "true"))
            || Integer.parseInt(settings.getOrDefault("sending.daily-limit", "0")) <= 0
            || Boolean.parseBoolean(settings.getOrDefault("sending.kill-switch", "true"));
    if (sendingProperties.blocksRealSending() || databaseBlocked) {
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.POLICY_BLOCK,
          "SENDING_BLOCKED",
          "Sending remains blocked by environment or database policy");
    }
    return OutboxProcessingResult.failure(
        OutboxErrorCategory.CONFIGURATION_BLOCK,
        "PROVIDER_NOT_CONNECTED",
        "No real provider is connected");
  }

  private OutboxProcessingResult currentMessagePolicy(OutboxWorkerService.ClaimedEvent event) {
    MessageSnapshot message =
        jdbcTemplate
            .query(
                """
                SELECT p.status AS prospect_status, p.contact_eligible, m.provider,
                  cc.valid AS channel_valid,
                  COALESCE(cc.consent, c.consent, 'UNKNOWN') AS consent,
                  c.deleted_at IS NOT NULL AS contact_deleted,
                  cp.status AS campaign_status,
                  EXISTS (
                    SELECT 1 FROM exclusion e
                    WHERE e.organization_id = m.organization_id
                      AND e.channel_type = m.channel
                      AND e.normalized_value = cc.normalized_value
                  ) AS excluded,
                  EXISTS (
                    SELECT 1 FROM organization_membership om
                    JOIN role_permission rp ON rp.role_id = om.role_id
                    WHERE om.organization_id = m.organization_id AND om.user_id = m.created_by
                      AND om.active = TRUE
                      AND rp.permission_code IN ('MESSAGE_DRAFT', 'MESSAGE_SIMULATE')
                  ) AS actor_authorized
                FROM message_record m
                JOIN prospect p ON p.id = m.prospect_id AND p.organization_id = m.organization_id
                LEFT JOIN contact c ON c.id = m.contact_id AND c.organization_id = m.organization_id
                LEFT JOIN contact_channel cc ON cc.id = m.contact_channel_id
                  AND cc.organization_id = m.organization_id
                LEFT JOIN campaign cp ON cp.id = m.campaign_id
                  AND cp.organization_id = m.organization_id
                WHERE m.organization_id = ? AND m.id = ?
                """,
                (rs, rowNum) ->
                    new MessageSnapshot(
                        rs.getString("prospect_status"),
                        rs.getBoolean("contact_eligible"),
                        rs.getString("provider"),
                        rs.getObject("channel_valid") == null || rs.getBoolean("channel_valid"),
                        rs.getString("consent"),
                        rs.getBoolean("contact_deleted"),
                        rs.getString("campaign_status"),
                        rs.getBoolean("excluded"),
                        rs.getBoolean("actor_authorized")),
                event.organizationId(),
                event.aggregateId())
            .stream()
            .findFirst()
            .orElse(null);
    if (message == null) {
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.NON_RETRYABLE,
          "MESSAGE_NOT_FOUND",
          "Message result no longer exists in this organization");
    }
    if ("CANCELLED".equals(message.campaignStatus()) || "PAUSED".equals(message.campaignStatus())) {
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.CANCELLED, "CAMPAIGN_INACTIVE", "Campaign is paused or cancelled");
    }
    if (message.excluded()) {
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.POLICY_BLOCK,
          "BLOCKED_BY_EXCLUSION",
          "Recipient became excluded after enqueue");
    }
    if (!message.contactEligible()
        || message.contactDeleted()
        || !message.channelValid()
        || "DENIED".equals(message.consent())
        || "CUSTOMER".equals(message.prospectStatus())
        || "DO_NOT_CONTACT".equals(message.prospectStatus())) {
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.POLICY_BLOCK,
          "BLOCKED_BY_POLICY",
          "Current prospect or contact policy does not permit processing");
    }
    if (!message.actorAuthorized()) {
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.POLICY_BLOCK,
          "BLOCKED_BY_PERMISSION",
          "Creating actor no longer has a messaging permission");
    }
    if (Set.of("GMAIL", "WHATSAPP_CLOUD").contains(message.provider())) {
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.CONFIGURATION_BLOCK,
          "PROVIDER_NOT_CONNECTED",
          "Real provider processing remains disconnected");
    }
    return null;
  }

  private java.util.UUID requiredUuid(OutboxWorkerService.ClaimedEvent event, String fieldName) {
    Object value = payload(event).get(fieldName);
    if (value == null) {
      throw new IllegalArgumentException("Outbox payload is missing " + fieldName);
    }
    return java.util.UUID.fromString(String.valueOf(value));
  }

  private Map<String, Object> payload(OutboxWorkerService.ClaimedEvent event) {
    try {
      return objectMapper.readValue(event.payload(), new TypeReference<>() {});
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Outbox payload is invalid", exception);
    }
  }

  private record MessageSnapshot(
      String prospectStatus,
      boolean contactEligible,
      String provider,
      boolean channelValid,
      String consent,
      boolean contactDeleted,
      String campaignStatus,
      boolean excluded,
      boolean actorAuthorized) {}
}
