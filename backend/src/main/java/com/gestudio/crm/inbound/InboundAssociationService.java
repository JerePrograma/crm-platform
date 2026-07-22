package com.gestudio.crm.inbound;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class InboundAssociationService {

  private final JdbcTemplate jdbcTemplate;

  public InboundAssociationService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Association associate(UUID organizationId, UUID inboundId) {
    ReceiptReference receipt = receipt(organizationId, inboundId);
    if (receipt.prospectId() != null) {
      return validatedManual(
          organizationId, receipt.prospectId(), receipt.contactId(), "MANUAL_ASSOCIATION");
    }
    Association byThread =
        unique(
            jdbcTemplate.query(
                """
                SELECT DISTINCT m.prospect_id, m.contact_id
                FROM message_record m
                WHERE m.organization_id = ? AND m.external_thread_id = ?
                ORDER BY m.prospect_id, m.contact_id NULLS LAST
                LIMIT 2
                """,
                this::association,
                organizationId,
                receipt.externalThreadId()),
            "EXTERNAL_THREAD");
    if (byThread != null) {
      return byThread;
    }
    if (receipt.inReplyToMessageId() != null && !receipt.inReplyToMessageId().isBlank()) {
      Association byMessage =
          unique(
              jdbcTemplate.query(
                  """
                  SELECT DISTINCT m.prospect_id, m.contact_id
                  FROM message_record m
                  WHERE m.organization_id = ? AND m.external_message_id = ?
                  ORDER BY m.prospect_id, m.contact_id NULLS LAST
                  LIMIT 2
                  """,
                  this::association,
                  organizationId,
                  receipt.inReplyToMessageId()),
              "RELATED_MESSAGE");
      if (byMessage != null) {
        return byMessage;
      }
    }
    List<Association> byContact =
        jdbcTemplate.query(
            """
            SELECT DISTINCT p.id AS prospect_id, c.id AS contact_id
            FROM contact_channel cc
            JOIN contact c ON c.id = cc.contact_id
              AND c.organization_id = cc.organization_id AND c.deleted_at IS NULL
            JOIN prospect p ON p.institution_id = c.institution_id
              AND p.organization_id = c.organization_id AND p.archived_at IS NULL
            WHERE cc.organization_id = ? AND cc.type = ? AND cc.normalized_value = ?
            ORDER BY p.id, c.id
            LIMIT 2
            """,
            this::association,
            organizationId,
            receipt.channel(),
            receipt.senderNormalized());
    return unique(byContact, "CONTACT_CHANNEL");
  }

  public Association validatedManual(
      UUID organizationId, UUID prospectId, UUID contactId, String method) {
    List<Association> matches;
    if (contactId == null) {
      matches =
          jdbcTemplate.query(
              """
              SELECT p.id AS prospect_id, NULL::uuid AS contact_id
              FROM prospect p
              WHERE p.organization_id = ? AND p.id = ? AND p.archived_at IS NULL
              """,
              this::association,
              organizationId,
              prospectId);
    } else {
      matches =
          jdbcTemplate.query(
              """
              SELECT p.id AS prospect_id, c.id AS contact_id
              FROM prospect p
              JOIN contact c ON c.id = ? AND c.organization_id = p.organization_id
                AND c.institution_id = p.institution_id AND c.deleted_at IS NULL
              WHERE p.organization_id = ? AND p.id = ? AND p.archived_at IS NULL
              """,
              this::association,
              contactId,
              organizationId,
              prospectId);
    }
    if (matches.size() != 1) {
      throw new IllegalArgumentException("Prospect and contact association is invalid");
    }
    Association match = matches.getFirst();
    return new Association(match.prospectId(), match.contactId(), method, null, false);
  }

  private ReceiptReference receipt(UUID organizationId, UUID inboundId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT external_thread_id, metadata->>'inReplyToMessageId' AS in_reply_to,
          channel, sender_normalized, prospect_id, contact_id
        FROM inbound_message WHERE organization_id = ? AND id = ?
        """,
        (rs, rowNum) ->
            new ReceiptReference(
                rs.getString("external_thread_id"),
                rs.getString("in_reply_to"),
                rs.getString("channel"),
                rs.getString("sender_normalized"),
                rs.getObject("prospect_id", UUID.class),
                rs.getObject("contact_id", UUID.class)),
        organizationId,
        inboundId);
  }

  private Association unique(List<Association> matches, String method) {
    if (matches.isEmpty()) {
      return null;
    }
    if (matches.size() > 1) {
      return new Association(null, null, method, "AMBIGUOUS_ASSOCIATION", true);
    }
    Association match = matches.getFirst();
    return new Association(match.prospectId(), match.contactId(), method, null, false);
  }

  private Association association(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    return new Association(
        rs.getObject("prospect_id", UUID.class),
        rs.getObject("contact_id", UUID.class),
        null,
        null,
        false);
  }

  private record ReceiptReference(
      String externalThreadId,
      String inReplyToMessageId,
      String channel,
      String senderNormalized,
      UUID prospectId,
      UUID contactId) {}

  public record Association(
      UUID prospectId, UUID contactId, String method, String reason, boolean ambiguous) {}
}
