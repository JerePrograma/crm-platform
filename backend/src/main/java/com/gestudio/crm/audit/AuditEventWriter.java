package com.gestudio.crm.audit;

import com.gestudio.crm.security.CurrentActor;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AuditEventWriter {

  private static final String INSERT_SQL =
      """
      INSERT INTO audit_event (
        id, version, created_at, updated_at, organization_id, actor_user_id,
        action, entity_type, entity_id, result, source, payload
      ) VALUES (?, 0, ?, ?, ?, ?, ?, ?, ?, ?, 'APPLICATION', CAST(? AS jsonb))
      """;

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final CurrentActor currentActor;

  public AuditEventWriter(
      JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, CurrentActor currentActor) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.currentActor = currentActor;
  }

  public UUID record(String action, String entityType, UUID entityId, Map<String, ?> payload) {
    return recordFor(
        currentActor.organizationId(),
        currentActor.userIdOrNull(),
        action,
        entityType,
        entityId,
        "SUCCESS",
        payload);
  }

  public UUID recordFor(
      UUID organizationId,
      UUID actorUserId,
      String action,
      String entityType,
      UUID entityId,
      String result,
      Map<String, ?> payload) {
    if (action == null || action.isBlank() || entityType == null || entityType.isBlank()) {
      throw new IllegalArgumentException("Audit action and entity type are required");
    }
    if (organizationId == null) {
      throw new IllegalArgumentException("Audit organization is required");
    }
    UUID auditId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());
    jdbcTemplate.update(
        INSERT_SQL,
        auditId,
        now,
        now,
        organizationId,
        actorUserId,
        action,
        entityType,
        entityId == null ? null : entityId.toString(),
        result == null ? "SUCCESS" : result,
        json(payload));
    return auditId;
  }

  private String json(Map<String, ?> payload) {
    try {
      return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Audit payload could not be serialized", exception);
    }
  }
}
