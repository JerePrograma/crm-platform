package com.gestudio.crm.inbound;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.CorrelationIds;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.outbox.OutboxPublisher;
import com.gestudio.crm.outbox.OutboxPublisher.PublishCommand;
import com.gestudio.crm.security.CurrentActor;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuarantineService {

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final InboundAssociationService associationService;
  private final OutboxPublisher outboxPublisher;
  private final AuditEventWriter auditEventWriter;
  private final Clock clock;

  public QuarantineService(
      JdbcTemplate jdbcTemplate,
      CurrentActor currentActor,
      InboundAssociationService associationService,
      OutboxPublisher outboxPublisher,
      AuditEventWriter auditEventWriter,
      Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.associationService = associationService;
    this.outboxPublisher = outboxPublisher;
    this.auditEventWriter = auditEventWriter;
    this.clock = clock;
  }

  @Transactional
  public void associate(UUID inboundId, UUID prospectId, UUID contactId) {
    associationService.validatedManual(
        currentActor.organizationId(), prospectId, contactId, "MANUAL_ASSOCIATION");
    requeue(inboundId, prospectId, contactId, "INBOUND_MANUALLY_ASSOCIATED");
  }

  @Transactional
  public void retry(UUID inboundId) {
    requeue(inboundId, null, null, "INBOUND_ASSOCIATION_RETRIED");
  }

  @Transactional
  public void discard(UUID inboundId, String reason) {
    if (reason == null || reason.isBlank() || reason.length() > 500) {
      throw new IllegalArgumentException(
          "Discard reason is required and limited to 500 characters");
    }
    Instant now = clock.instant();
    int updated =
        jdbcTemplate.update(
            """
            UPDATE inbound_message SET status = 'DISCARDED', association_status = 'DISCARDED',
              quarantine_reason = ?, processed_at = ?, discarded_at = ?
            WHERE organization_id = ? AND id = ? AND status = 'QUARANTINED'
            """,
            reason.trim(),
            Timestamp.from(now),
            Timestamp.from(now),
            currentActor.organizationId(),
            inboundId);
    requireUpdated(updated);
    auditEventWriter.record(
        "INBOUND_DISCARDED",
        "INBOUND_MESSAGE",
        inboundId,
        Map.of("reason", reason.trim(), "correlationId", CorrelationIds.currentOrCreate()));
  }

  private void requeue(UUID inboundId, UUID prospectId, UUID contactId, String action) {
    Instant now = clock.instant();
    Integer count =
        jdbcTemplate
            .query(
                """
                UPDATE inbound_message SET status = 'PENDING', association_status = ?,
                  prospect_id = ?, contact_id = ?, activity_id = NULL, quarantine_reason = NULL,
                  processed_at = NULL, discarded_at = NULL, requeue_count = requeue_count + 1
                WHERE organization_id = ? AND id = ? AND status = 'QUARANTINED'
                  AND requeue_count < 100
                RETURNING requeue_count
                """,
                (rs, rowNum) -> rs.getInt(1),
                prospectId == null ? "PENDING" : "ASSOCIATED",
                prospectId,
                contactId,
                currentActor.organizationId(),
                inboundId)
            .stream()
            .findFirst()
            .orElse(null);
    if (count == null) {
      throw new ResourceNotFoundException("Quarantined inbound message was not found");
    }
    String correlationId = CorrelationIds.currentOrCreate();
    outboxPublisher.publish(
        new PublishCommand(
            currentActor.organizationId(),
            "INBOUND_RECEIVED_V1",
            1,
            "INBOUND_MESSAGE",
            inboundId,
            Map.of("inboundMessageId", inboundId.toString()),
            "inbound-requeue:" + inboundId + ":" + count,
            correlationId,
            currentActor.userIdOrNull(),
            5));
    auditEventWriter.record(
        action,
        "INBOUND_MESSAGE",
        inboundId,
        Map.of(
            "prospectId", prospectId == null ? "" : prospectId.toString(),
            "requeueCount", count,
            "correlationId", correlationId));
  }

  private void requireUpdated(int updated) {
    if (updated != 1) {
      throw new ResourceNotFoundException("Quarantined inbound message was not found");
    }
  }
}
