package com.gestudio.crm.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditEventWriter {

  private static final String INSERT_SQL =
      """
      INSERT INTO audit_event (
        id, version, created_at, updated_at, action, entity_type, entity_id, payload
      ) VALUES (?, 0, ?, ?, ?, ?, ?, CAST(? AS jsonb))
      """;

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public AuditEventWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  public UUID record(
      String action, String entityType, UUID entityId, Map<String, ?> payload) {
    if (action == null || action.isBlank() || entityType == null || entityType.isBlank()) {
      throw new IllegalArgumentException("Audit action and entity type are required");
    }
    UUID auditId = UUID.randomUUID();
    Instant now = Instant.now();
    jdbcTemplate.update(
        INSERT_SQL,
        auditId,
        now,
        now,
        action,
        entityType,
        entityId == null ? null : entityId.toString(),
        json(payload));
    return auditId;
  }

  private String json(Map<String, ?> payload) {
    try {
      return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Audit payload could not be serialized", exception);
    }
  }
}
