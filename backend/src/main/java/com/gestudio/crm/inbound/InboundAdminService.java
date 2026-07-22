package com.gestudio.crm.inbound;

import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.security.CurrentActor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class InboundAdminService {

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final InboundProperties properties;

  public InboundAdminService(
      JdbcTemplate jdbcTemplate, CurrentActor currentActor, InboundProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.properties = properties;
  }

  public Page<InboundView> list(String status, int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(size, 100));
    if (status != null
        && !List.of("PENDING", "PROCESSING", "PROCESSED", "QUARANTINED", "DISCARDED", "FAILED")
            .contains(status)) {
      throw new IllegalArgumentException("Inbound status is invalid");
    }
    String filter = status == null ? "" : " AND status = ?";
    Object[] listParameters =
        status == null
            ? new Object[] {currentActor.organizationId(), safeSize, (long) safePage * safeSize}
            : new Object[] {
              currentActor.organizationId(), status, safeSize, (long) safePage * safeSize
            };
    List<InboundView> content =
        jdbcTemplate.query(
            "SELECT "
                + columns()
                + " FROM inbound_message WHERE organization_id = ?"
                + filter
                + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
            this::view,
            listParameters);
    Long total =
        status == null
            ? jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inbound_message WHERE organization_id = ?",
                Long.class,
                currentActor.organizationId())
            : jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inbound_message WHERE organization_id = ? AND status = ?",
                Long.class,
                currentActor.organizationId(),
                status);
    return new PageImpl<>(content, PageRequest.of(safePage, safeSize), total == null ? 0 : total);
  }

  public InboundView detail(UUID id) {
    return jdbcTemplate
        .query(
            "SELECT " + columns() + " FROM inbound_message WHERE organization_id = ? AND id = ?",
            this::view,
            currentActor.organizationId(),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Inbound message was not found"));
  }

  public WebhookHealth webhookHealth() {
    return new WebhookHealth(
        "FAKE_INBOUND",
        properties.enabled(),
        properties.configured(),
        properties.maxPayloadBytes());
  }

  private String columns() {
    return """
        id, provider, external_event_id, external_message_id, external_thread_id, channel,
        sender_normalized, recipient_normalized, received_at, payload_hash, status,
        association_status, prospect_id, contact_id, activity_id, quarantine_reason,
        requeue_count, correlation_id, created_at, processed_at, discarded_at
        """;
  }

  private InboundView view(ResultSet rs, int rowNum) throws SQLException {
    return new InboundView(
        rs.getObject("id", UUID.class),
        rs.getString("provider"),
        rs.getString("external_event_id"),
        rs.getString("external_message_id"),
        rs.getString("external_thread_id"),
        rs.getString("channel"),
        mask(rs.getString("sender_normalized")),
        mask(rs.getString("recipient_normalized")),
        instant(rs, "received_at"),
        rs.getString("payload_hash"),
        rs.getString("status"),
        rs.getString("association_status"),
        rs.getObject("prospect_id", UUID.class),
        rs.getObject("contact_id", UUID.class),
        rs.getObject("activity_id", UUID.class),
        rs.getString("quarantine_reason"),
        rs.getInt("requeue_count"),
        rs.getString("correlation_id"),
        instant(rs, "created_at"),
        instant(rs, "processed_at"),
        instant(rs, "discarded_at"));
  }

  private String mask(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    if (value.length() <= 4) {
      return "****";
    }
    return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
  }

  private Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  public record InboundView(
      UUID id,
      String provider,
      String externalEventId,
      String externalMessageId,
      String externalThreadId,
      String channel,
      String senderMasked,
      String recipientMasked,
      Instant receivedAt,
      String payloadHash,
      String status,
      String associationStatus,
      UUID prospectId,
      UUID contactId,
      UUID activityId,
      String quarantineReason,
      int requeueCount,
      String correlationId,
      Instant createdAt,
      Instant processedAt,
      Instant discardedAt) {}

  public record WebhookHealth(
      String provider, boolean enabled, boolean configured, int maxPayloadBytes) {}
}
