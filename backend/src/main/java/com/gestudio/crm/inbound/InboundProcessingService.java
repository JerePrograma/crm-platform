package com.gestudio.crm.inbound;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.inbound.InboundAssociationService.Association;
import com.gestudio.crm.prospect.ProspectLifecycle;
import com.gestudio.crm.prospect.ProspectStatus;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboundProcessingService {

  private final JdbcTemplate jdbcTemplate;
  private final InboundAssociationService associationService;
  private final ProspectLifecycle prospectLifecycle;
  private final AuditEventWriter auditEventWriter;
  private final Clock clock;

  public InboundProcessingService(
      JdbcTemplate jdbcTemplate,
      InboundAssociationService associationService,
      ProspectLifecycle prospectLifecycle,
      AuditEventWriter auditEventWriter,
      Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.associationService = associationService;
    this.prospectLifecycle = prospectLifecycle;
    this.auditEventWriter = auditEventWriter;
    this.clock = clock;
  }

  @Transactional
  public void process(UUID organizationId, UUID inboundId, String correlationId) {
    InboundReceipt receipt = receipt(organizationId, inboundId);
    if (receipt.status().equals("PROCESSED") || receipt.status().equals("DISCARDED")) {
      return;
    }
    int claimed =
        jdbcTemplate.update(
            """
            UPDATE inbound_message SET status = 'PROCESSING'
            WHERE organization_id = ? AND id = ? AND status IN ('PENDING', 'QUARANTINED', 'FAILED')
            """,
            organizationId,
            inboundId);
    if (claimed == 0 && !receipt.status().equals("PROCESSING")) {
      return;
    }

    Association association = associationService.associate(organizationId, inboundId);
    Instant now = clock.instant();
    if (association == null || association.ambiguous()) {
      String reason = association == null ? "NO_ASSOCIATION_FOUND" : association.reason();
      jdbcTemplate.update(
          """
          UPDATE inbound_message SET status = 'QUARANTINED',
            association_status = ?, quarantine_reason = ?, processed_at = ?
          WHERE organization_id = ? AND id = ?
          """,
          association == null ? "NOT_FOUND" : "AMBIGUOUS",
          reason,
          Timestamp.from(now),
          organizationId,
          inboundId);
      auditEventWriter.recordFor(
          organizationId,
          null,
          "INBOUND_QUARANTINED",
          "INBOUND_MESSAGE",
          inboundId,
          "SUCCESS",
          Map.of("reason", reason, "correlationId", correlationId));
      return;
    }

    ProspectContext prospect = prospect(organizationId, association.prospectId());
    UUID activityId =
        createActivity(organizationId, inboundId, receipt, association, correlationId, now);
    createInboundMessageRecord(organizationId, inboundId, receipt, association, now);
    jdbcTemplate.update(
        """
        UPDATE prospect SET last_contact_at = ?, updated_at = ?, version = version + 1
        WHERE organization_id = ? AND id = ?
        """,
        Timestamp.from(receipt.receivedAt()),
        Timestamp.from(now),
        organizationId,
        association.prospectId());
    boolean transitioned = transitionToReplied(organizationId, prospect, now);
    int cancelled = cancelPendingOutbound(organizationId, association.prospectId(), now);
    UUID taskId = createTask(organizationId, association.prospectId(), prospect.ownerUserId(), now);
    jdbcTemplate.update(
        """
        UPDATE inbound_message SET status = 'PROCESSED', association_status = 'ASSOCIATED',
          prospect_id = ?, contact_id = ?, activity_id = ?, quarantine_reason = NULL,
          processed_at = ?
        WHERE organization_id = ? AND id = ?
        """,
        association.prospectId(),
        association.contactId(),
        activityId,
        Timestamp.from(now),
        organizationId,
        inboundId);
    auditEventWriter.recordFor(
        organizationId,
        null,
        "INBOUND_PROCESSED",
        "INBOUND_MESSAGE",
        inboundId,
        "SUCCESS",
        Map.of(
            "prospectId",
            association.prospectId().toString(),
            "association",
            association.method(),
            "transitionedToReplied",
            transitioned,
            "cancelledPendingSequenceEvents",
            cancelled,
            "taskId",
            taskId.toString(),
            "correlationId",
            correlationId));
  }

  private InboundReceipt receipt(UUID organizationId, UUID inboundId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT status, external_event_id, external_message_id, external_thread_id,
          channel, body_excerpt, payload_hash, received_at
        FROM inbound_message WHERE organization_id = ? AND id = ?
        """,
        (rs, rowNum) ->
            new InboundReceipt(
                rs.getString("status"),
                rs.getString("external_event_id"),
                rs.getString("external_message_id"),
                rs.getString("external_thread_id"),
                rs.getString("channel"),
                rs.getString("body_excerpt"),
                rs.getString("payload_hash"),
                rs.getTimestamp("received_at").toInstant()),
        organizationId,
        inboundId);
  }

  private ProspectContext prospect(UUID organizationId, UUID prospectId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT p.status, COALESCE(p.owner_user_id, (
          SELECT om.user_id FROM organization_membership om
          JOIN app_user u ON u.id = om.user_id
          WHERE om.organization_id = p.organization_id AND om.active = TRUE AND u.active = TRUE
          ORDER BY om.created_at, om.user_id LIMIT 1
        )) AS owner_user_id
        FROM prospect p WHERE p.organization_id = ? AND p.id = ?
        """,
        (rs, rowNum) ->
            new ProspectContext(
                ProspectStatus.valueOf(rs.getString("status")),
                rs.getObject("owner_user_id", UUID.class),
                prospectId),
        organizationId,
        prospectId);
  }

  private UUID createActivity(
      UUID organizationId,
      UUID inboundId,
      InboundReceipt receipt,
      Association association,
      String correlationId,
      Instant now) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO activity (
          id, organization_id, prospect_id, contact_id, actor_user_id, activity_type,
          occurred_at, channel, direction, outcome, summary, detail, external_reference,
          metadata, created_at
        ) VALUES (?, ?, ?, ?, NULL, ?, ?, ?, 'INBOUND', 'RECEIVED',
          'Respuesta inbound recibida', ?, ?, CAST(? AS jsonb), ?)
        """,
        id,
        organizationId,
        association.prospectId(),
        association.contactId(),
        receipt.channel().equals("EMAIL") ? "EMAIL_RECEIVED" : "WHATSAPP_RECEIVED",
        Timestamp.from(receipt.receivedAt()),
        receipt.channel(),
        receipt.bodyExcerpt(),
        "fake-inbound:" + receipt.externalEventId(),
        "{\"provider\":\"FAKE_INBOUND\",\"correlationId\":\"" + correlationId + "\"}",
        Timestamp.from(now));
    return id;
  }

  private void createInboundMessageRecord(
      UUID organizationId,
      UUID inboundId,
      InboundReceipt receipt,
      Association association,
      Instant now) {
    jdbcTemplate.update(
        """
        INSERT INTO message_record (
          id, version, organization_id, prospect_id, contact_id, channel, direction,
          status, subject, body_text, provider, external_message_id, external_thread_id,
          idempotency_key, request_hash, created_at, updated_at
        ) VALUES (?, 0, ?, ?, ?, ?, 'INBOUND', 'RECEIVED', NULL, ?, 'FAKE', ?, ?, ?, ?, ?, ?)
        ON CONFLICT (organization_id, idempotency_key) DO NOTHING
        """,
        UUID.randomUUID(),
        organizationId,
        association.prospectId(),
        association.contactId(),
        receipt.channel(),
        receipt.bodyExcerpt(),
        receipt.externalMessageId(),
        receipt.externalThreadId(),
        "inbound-message:" + inboundId,
        receipt.payloadHash(),
        Timestamp.from(now),
        Timestamp.from(now));
  }

  private boolean transitionToReplied(UUID organizationId, ProspectContext prospect, Instant now) {
    if (prospect.status() == ProspectStatus.CUSTOMER
        || prospect.status() == ProspectStatus.DO_NOT_CONTACT
        || !prospectLifecycle.allowedFrom(prospect.status()).contains(ProspectStatus.REPLIED)) {
      return false;
    }
    jdbcTemplate.update(
        """
        UPDATE prospect SET status = 'REPLIED', status_detail_at = ?, updated_at = ?,
          version = version + 1 WHERE organization_id = ? AND id = ?
        """,
        Timestamp.from(now),
        Timestamp.from(now),
        organizationId,
        prospect.prospectId());
    jdbcTemplate.update(
        """
        INSERT INTO prospect_status_history (
          id, organization_id, prospect_id, actor_user_id, previous_status, new_status,
          reason, comment, source, created_at
        ) VALUES (?, ?, ?, NULL, ?, 'REPLIED', 'INBOUND_RESPONSE',
          'Transición automática por respuesta inbound verificada', 'INBOUND', ?)
        """,
        UUID.randomUUID(),
        organizationId,
        prospect.prospectId(),
        prospect.status().name(),
        Timestamp.from(now));
    return true;
  }

  private int cancelPendingOutbound(UUID organizationId, UUID prospectId, Instant now) {
    return jdbcTemplate.update(
        """
        UPDATE outbox_event SET status = 'CANCELLED', processed_at = ?, updated_at = ?,
          last_error_code = 'INBOUND_REPLY',
          last_error_summary = 'Cancelled because the prospect replied'
        WHERE organization_id = ? AND status IN ('PENDING', 'RETRY')
          AND event_type IN ('MESSAGE_SEND_REQUESTED_V1', 'SEQUENCE_STEP_DUE_V1')
          AND payload->>'prospectId' = ?
        """,
        Timestamp.from(now),
        Timestamp.from(now),
        organizationId,
        prospectId.toString());
  }

  private UUID createTask(UUID organizationId, UUID prospectId, UUID ownerUserId, Instant now) {
    if (ownerUserId == null) {
      throw new IllegalStateException("Inbound task requires an active organization member");
    }
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO crm_task (
          id, version, organization_id, prospect_id, owner_user_id, creator_user_id,
          title, description, due_at, priority, status, task_type, created_at, updated_at
        ) VALUES (?, 0, ?, ?, ?, NULL, 'Responder mensaje inbound',
          'Revisar y responder manualmente; no se ejecutó ninguna respuesta automática',
          ?, 'HIGH', 'OPEN', 'INBOUND_REPLY', ?, ?)
        """,
        id,
        organizationId,
        prospectId,
        ownerUserId,
        Timestamp.from(now.plusSeconds(86400)),
        Timestamp.from(now),
        Timestamp.from(now));
    return id;
  }

  private record InboundReceipt(
      String status,
      String externalEventId,
      String externalMessageId,
      String externalThreadId,
      String channel,
      String bodyExcerpt,
      String payloadHash,
      Instant receivedAt) {}

  private record ProspectContext(ProspectStatus status, UUID ownerUserId, UUID prospectId) {}
}
