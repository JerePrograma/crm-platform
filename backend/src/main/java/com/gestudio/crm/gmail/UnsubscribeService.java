package com.gestudio.crm.gmail;

import com.gestudio.crm.audit.AuditEventWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnsubscribeService {

  private final JdbcTemplate jdbcTemplate;
  private final UnsubscribeTokenService tokens;
  private final AuditEventWriter auditEventWriter;

  public UnsubscribeService(
      JdbcTemplate jdbcTemplate,
      UnsubscribeTokenService tokens,
      AuditEventWriter auditEventWriter) {
    this.jdbcTemplate = jdbcTemplate;
    this.tokens = tokens;
    this.auditEventWriter = auditEventWriter;
  }

  @Transactional(readOnly = true)
  public boolean valid(UUID tokenId, String presentedToken) {
    TokenRow row = find(tokenId, false);
    return row != null && verify(row, presentedToken);
  }

  @Transactional
  public boolean unsubscribe(UUID tokenId, String presentedToken) {
    TokenRow row = find(tokenId, true);
    if (row == null || !verify(row, presentedToken)) {
      return false;
    }
    jdbcTemplate.query(
        "SELECT pg_advisory_xact_lock(hashtextextended(?::text, 0))",
        resultSet -> null,
        row.contactChannelId().toString());
    if (row.used()) {
      return true;
    }
    Channel channel =
        jdbcTemplate
            .query(
                """
                SELECT cc.type, cc.normalized_value, p.id AS prospect_id
                FROM contact_channel cc
                JOIN contact c ON c.id = cc.contact_id AND c.organization_id = cc.organization_id
                JOIN prospect p ON p.institution_id = c.institution_id
                  AND p.organization_id = cc.organization_id
                WHERE cc.id = ? AND cc.organization_id = ?
                """,
                (rs, ignored) ->
                    new Channel(
                        rs.getString("type"),
                        rs.getString("normalized_value"),
                        rs.getObject("prospect_id", UUID.class)),
                row.contactChannelId(),
                row.organizationId())
            .stream()
            .findFirst()
            .orElse(null);
    if (channel == null) {
      return false;
    }
    jdbcTemplate.update(
        """
        INSERT INTO exclusion (
          id, version, organization_id, channel_type, normalized_value, reason,
          created_at, updated_at
        ) VALUES (?, 0, ?, ?, ?, 'UNSUBSCRIBE_REQUEST', now(), now())
        ON CONFLICT (organization_id, channel_type, normalized_value) DO NOTHING
        """,
        UUID.randomUUID(),
        row.organizationId(),
        channel.type(),
        channel.normalizedValue());
    jdbcTemplate.update(
        """
        INSERT INTO global_contact_suppression (
          id, channel_type, value_hash, reason, created_at, updated_at
        ) VALUES (?, ?, ?, 'UNSUBSCRIBED', now(), now())
        ON CONFLICT (channel_type, value_hash) DO NOTHING
        """,
        UUID.randomUUID(),
        channel.type(),
        tokens.suppressionHash(channel.normalizedValue()));
    int cancelled =
        jdbcTemplate.update(
            """
            WITH cancelled_messages AS (
              UPDATE message_record SET status = 'CANCELLED', result_category = 'UNSUBSCRIBED',
                last_error_summary = 'Recipient unsubscribed', next_attempt_at = NULL,
                updated_at = now(), version = version + 1
              WHERE organization_id = ? AND contact_channel_id = ?
                AND status IN ('PENDING', 'SCHEDULED', 'RETRYABLE')
              RETURNING id
            )
            UPDATE outbox_event SET status = 'CANCELLED', processed_at = now(), updated_at = now(),
              last_error_code = 'UNSUBSCRIBED', last_error_summary = 'Recipient unsubscribed'
            WHERE organization_id = ? AND aggregate_id IN (SELECT id FROM cancelled_messages)
              AND status IN ('PENDING', 'RETRY')
            """,
            row.organizationId(),
            row.contactChannelId(),
            row.organizationId());
    int followUpsCancelled =
        jdbcTemplate.update(
            """
            UPDATE crm_task SET status = 'CANCELLED', completed_at = NULL, cancelled_at = now(),
              outcome = 'Recipient unsubscribed', updated_at = now(), version = version + 1
            WHERE organization_id = ? AND prospect_id = ? AND task_type = 'FOLLOW_UP'
              AND status IN ('OPEN', 'IN_PROGRESS')
            """,
            row.organizationId(),
            channel.prospectId());
    jdbcTemplate.update(
        "UPDATE unsubscribe_token SET used_at = now() WHERE id = ? AND used_at IS NULL", tokenId);
    auditEventWriter.recordFor(
        row.organizationId(),
        null,
        "UNSUBSCRIBE_PROCESSED",
        "CAMPAIGN",
        row.campaignId(),
        "SUCCESS",
        Map.of(
            "channel",
            channel.type(),
            "pendingMessagesCancelled",
            cancelled,
            "followUpsCancelled",
            followUpsCancelled));
    return true;
  }

  private TokenRow find(UUID tokenId, boolean lock) {
    return jdbcTemplate
        .query(
            """
            SELECT id, organization_id, campaign_id, message_id, contact_channel_id,
              token_hash, key_id, used_at IS NOT NULL AS used
            FROM unsubscribe_token WHERE id = ?
            """
                + (lock ? " FOR UPDATE" : ""),
            this::tokenRow,
            tokenId)
        .stream()
        .findFirst()
        .orElse(null);
  }

  private boolean verify(TokenRow row, String presentedToken) {
    return tokens.verify(
        row.id(),
        row.organizationId(),
        row.messageId(),
        presentedToken,
        row.tokenHash(),
        row.keyId());
  }

  private TokenRow tokenRow(ResultSet rs, int ignored) throws SQLException {
    return new TokenRow(
        rs.getObject("id", UUID.class),
        rs.getObject("organization_id", UUID.class),
        rs.getObject("campaign_id", UUID.class),
        rs.getObject("message_id", UUID.class),
        rs.getObject("contact_channel_id", UUID.class),
        rs.getString("token_hash"),
        rs.getString("key_id"),
        rs.getBoolean("used"));
  }

  private record TokenRow(
      UUID id,
      UUID organizationId,
      UUID campaignId,
      UUID messageId,
      UUID contactChannelId,
      String tokenHash,
      String keyId,
      boolean used) {}

  private record Channel(String type, String normalizedValue, UUID prospectId) {}
}
