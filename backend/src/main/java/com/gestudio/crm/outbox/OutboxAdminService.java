package com.gestudio.crm.outbox;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.CorrelationIds;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.security.CurrentActor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxAdminService {

  private static final Map<String, String> SORTS =
      Map.of(
          "createdAt,desc", "created_at DESC, id DESC",
          "createdAt,asc", "created_at, id",
          "nextAttemptAt,asc", "next_attempt_at, id",
          "status,asc", "status, created_at DESC, id DESC");

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final AuditEventWriter auditEventWriter;
  private final Clock clock;

  public OutboxAdminService(
      JdbcTemplate jdbcTemplate,
      CurrentActor currentActor,
      AuditEventWriter auditEventWriter,
      Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.auditEventWriter = auditEventWriter;
    this.clock = clock;
  }

  public Page<OutboxView> list(
      OutboxStatus status,
      String eventType,
      String aggregateType,
      UUID aggregateId,
      Instant from,
      Instant to,
      int page,
      int size,
      String sort) {
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(size, 100));
    String order = SORTS.get(sort == null ? "createdAt,desc" : sort);
    if (order == null) {
      throw new IllegalArgumentException("Outbox sort is not allowed");
    }
    Query query = filters(status, eventType, aggregateType, aggregateId, from, to);
    List<Object> pageParameters = new ArrayList<>(query.parameters());
    pageParameters.add(safeSize);
    pageParameters.add((long) safePage * safeSize);
    List<OutboxView> content =
        jdbcTemplate.query(
            "SELECT " + columns() + query.sql() + " ORDER BY " + order + " LIMIT ? OFFSET ?",
            this::view,
            pageParameters.toArray());
    Long total =
        jdbcTemplate.queryForObject(
            "SELECT count(*)" + query.sql(), Long.class, query.parameters().toArray());
    return new PageImpl<>(content, PageRequest.of(safePage, safeSize), total == null ? 0 : total);
  }

  public OutboxView detail(UUID id) {
    return jdbcTemplate
        .query(
            "SELECT " + columns() + " FROM outbox_event WHERE organization_id = ? AND id = ?",
            this::view,
            currentActor.organizationId(),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Outbox event was not found"));
  }

  public List<StatusMetric> metrics() {
    return jdbcTemplate.query(
        """
        SELECT status, count(*) AS event_count, min(created_at) AS oldest_created_at
        FROM outbox_event WHERE organization_id = ? GROUP BY status ORDER BY status
        """,
        (rs, rowNum) ->
            new StatusMetric(
                OutboxStatus.valueOf(rs.getString("status")),
                rs.getLong("event_count"),
                instant(rs, "oldest_created_at")),
        currentActor.organizationId());
  }

  @Transactional
  public OutboxView requeue(UUID id) {
    Instant now = clock.instant();
    int updated =
        jdbcTemplate.update(
            """
            UPDATE outbox_event SET status = 'PENDING', attempt_count = 0,
              next_attempt_at = ?, updated_at = ?, processed_at = NULL,
              locked_at = NULL, lock_expires_at = NULL, locked_by = NULL,
              last_error_code = NULL, last_error_summary = NULL, result_summary = NULL
            WHERE organization_id = ? AND id = ? AND status = 'DEAD'
            """,
            Timestamp.from(now),
            Timestamp.from(now),
            currentActor.organizationId(),
            id);
    requireUpdated(updated, "Only DEAD events can be requeued");
    auditEventWriter.record(
        "OUTBOX_REQUEUED",
        "OUTBOX_EVENT",
        id,
        Map.of("correlationId", CorrelationIds.currentOrCreate()));
    return detail(id);
  }

  @Transactional
  public OutboxView cancel(UUID id) {
    Instant now = clock.instant();
    int updated =
        jdbcTemplate.update(
            """
            UPDATE outbox_event SET status = 'CANCELLED', updated_at = ?, processed_at = ?,
              last_error_code = 'MANUAL_CANCEL', last_error_summary = 'Cancelled by an authorized user'
            WHERE organization_id = ? AND id = ? AND status IN ('PENDING', 'RETRY')
            """,
            Timestamp.from(now),
            Timestamp.from(now),
            currentActor.organizationId(),
            id);
    requireUpdated(updated, "Only PENDING or RETRY events can be cancelled");
    auditEventWriter.record(
        "OUTBOX_CANCELLED",
        "OUTBOX_EVENT",
        id,
        Map.of("correlationId", CorrelationIds.currentOrCreate()));
    return detail(id);
  }

  @Transactional
  public boolean pause(boolean paused) {
    Instant now = clock.instant();
    jdbcTemplate.update(
        """
        INSERT INTO system_setting (
          id, version, organization_id, created_at, updated_at, setting_key, setting_value
        ) VALUES (?, 0, ?, ?, ?, 'outbox-worker-paused', ?)
        ON CONFLICT (organization_id, setting_key) DO UPDATE
          SET setting_value = EXCLUDED.setting_value, updated_at = EXCLUDED.updated_at,
            version = system_setting.version + 1
        """,
        UUID.randomUUID(),
        currentActor.organizationId(),
        Timestamp.from(now),
        Timestamp.from(now),
        Boolean.toString(paused));
    auditEventWriter.record(
        paused ? "OUTBOX_WORKER_PAUSED" : "OUTBOX_WORKER_RESUMED",
        "OUTBOX_WORKER",
        null,
        Map.of("correlationId", CorrelationIds.currentOrCreate()));
    return paused;
  }

  public boolean paused() {
    return jdbcTemplate
        .query(
            """
            SELECT setting_value FROM system_setting
            WHERE organization_id = ? AND setting_key = 'outbox-worker-paused'
            """,
            (rs, rowNum) -> Boolean.parseBoolean(rs.getString(1)),
            currentActor.organizationId())
        .stream()
        .findFirst()
        .orElse(false);
  }

  private Query filters(
      OutboxStatus status,
      String eventType,
      String aggregateType,
      UUID aggregateId,
      Instant from,
      Instant to) {
    StringBuilder sql = new StringBuilder(" FROM outbox_event WHERE organization_id = ?");
    List<Object> parameters = new ArrayList<>();
    parameters.add(currentActor.organizationId());
    if (status != null) {
      sql.append(" AND status = ?");
      parameters.add(status.name());
    }
    if (eventType != null && !eventType.isBlank()) {
      sql.append(" AND event_type = ?");
      parameters.add(eventType);
    }
    if (aggregateType != null && !aggregateType.isBlank()) {
      sql.append(" AND aggregate_type = ?");
      parameters.add(aggregateType);
    }
    if (aggregateId != null) {
      sql.append(" AND aggregate_id = ?");
      parameters.add(aggregateId);
    }
    if (from != null) {
      sql.append(" AND created_at >= ?");
      parameters.add(Timestamp.from(from));
    }
    if (to != null) {
      sql.append(" AND created_at < ?");
      parameters.add(Timestamp.from(to));
    }
    return new Query(sql.toString(), parameters);
  }

  private String columns() {
    return """
        id, event_type, event_version, aggregate_type, aggregate_id, payload::text,
        status, attempt_count, max_attempts, next_attempt_at, locked_at, lock_expires_at,
        locked_by, created_at, updated_at, processed_at, last_error_code,
        last_error_summary, result_summary, idempotency_key, correlation_id
        """;
  }

  private OutboxView view(ResultSet rs, int rowNum) throws SQLException {
    return new OutboxView(
        rs.getObject("id", UUID.class),
        rs.getString("event_type"),
        rs.getInt("event_version"),
        rs.getString("aggregate_type"),
        rs.getObject("aggregate_id", UUID.class),
        rs.getString("payload"),
        OutboxStatus.valueOf(rs.getString("status")),
        rs.getInt("attempt_count"),
        rs.getInt("max_attempts"),
        instant(rs, "next_attempt_at"),
        instant(rs, "locked_at"),
        instant(rs, "lock_expires_at"),
        rs.getString("locked_by"),
        instant(rs, "created_at"),
        instant(rs, "updated_at"),
        instant(rs, "processed_at"),
        rs.getString("last_error_code"),
        rs.getString("last_error_summary"),
        rs.getString("result_summary"),
        rs.getString("idempotency_key"),
        rs.getString("correlation_id"));
  }

  private Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private void requireUpdated(int updated, String message) {
    if (updated != 1) {
      throw new ResourceNotFoundException(message);
    }
  }

  private record Query(String sql, List<Object> parameters) {}

  public record OutboxView(
      UUID id,
      String eventType,
      int eventVersion,
      String aggregateType,
      UUID aggregateId,
      String payload,
      OutboxStatus status,
      int attemptCount,
      int maxAttempts,
      Instant nextAttemptAt,
      Instant lockedAt,
      Instant lockExpiresAt,
      String lockedBy,
      Instant createdAt,
      Instant updatedAt,
      Instant processedAt,
      String lastErrorCode,
      String lastErrorSummary,
      String resultSummary,
      String idempotencyKey,
      String correlationId) {}

  public record StatusMetric(OutboxStatus status, long count, Instant oldestCreatedAt) {}
}
