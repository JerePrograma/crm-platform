package com.gestudio.crm.inbound;

import com.gestudio.crm.common.OptimisticConflictException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReplayProtectionService {

  private final JdbcTemplate jdbcTemplate;

  public ReplayProtectionService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public ExistingReceipt check(
      UUID organizationId, String provider, String externalEventId, String nonceHash) {
    ExistingReceipt event =
        jdbcTemplate
            .query(
                """
                SELECT id, payload_hash, status FROM inbound_message
                WHERE organization_id = ? AND provider = ? AND external_event_id = ?
                """,
                (rs, rowNum) ->
                    new ExistingReceipt(
                        rs.getObject("id", UUID.class),
                        rs.getString("payload_hash"),
                        rs.getString("status")),
                organizationId,
                provider,
                externalEventId)
            .stream()
            .findFirst()
            .orElse(null);
    if (event != null) {
      return event;
    }
    Integer nonceCount =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM inbound_message
            WHERE organization_id = ? AND provider = ? AND nonce_hash = ?
            """,
            Integer.class,
            organizationId,
            provider,
            nonceHash);
    if (nonceCount != null && nonceCount > 0) {
      throw new OptimisticConflictException("Webhook nonce was already used");
    }
    return null;
  }

  public record ExistingReceipt(UUID id, String payloadHash, String status) {}
}
