package com.gestudio.crm.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

  private static final String SELECT_RECENT =
      """
      SELECT id, created_at, action, entity_type, entity_id, payload::text
      FROM audit_event
      ORDER BY created_at DESC
      LIMIT ?
      """;

  private final JdbcTemplate jdbcTemplate;

  public AuditQueryService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional(readOnly = true)
  public List<AuditEventView> recent(int requestedLimit) {
    int limit = Math.max(1, Math.min(requestedLimit, 500));
    return jdbcTemplate.query(
        SELECT_RECENT,
        (resultSet, rowNumber) ->
            new AuditEventView(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class).toInstant(),
                resultSet.getString("action"),
                resultSet.getString("entity_type"),
                resultSet.getString("entity_id"),
                resultSet.getString("payload")),
        limit);
  }

  public record AuditEventView(
      UUID id,
      Instant createdAt,
      String action,
      String entityType,
      String entityId,
      String payload) {}
}
