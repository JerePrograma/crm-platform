package com.gestudio.crm.audit;

import com.gestudio.crm.security.CurrentActor;
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
      SELECT id, created_at, actor_user_id, action, entity_type, entity_id,
             result, correlation_id, payload::text
      FROM audit_event
      WHERE organization_id = ?
      ORDER BY created_at DESC
      LIMIT ?
      """;

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;

  public AuditQueryService(JdbcTemplate jdbcTemplate, CurrentActor currentActor) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
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
                resultSet.getObject("actor_user_id", UUID.class),
                resultSet.getString("action"),
                resultSet.getString("entity_type"),
                resultSet.getString("entity_id"),
                resultSet.getString("result"),
                resultSet.getString("correlation_id"),
                resultSet.getString("payload")),
        currentActor.organizationId(),
        limit);
  }

  public record AuditEventView(
      UUID id,
      Instant createdAt,
      UUID actorUserId,
      String action,
      String entityType,
      String entityId,
      String result,
      String correlationId,
      String payload) {}
}
