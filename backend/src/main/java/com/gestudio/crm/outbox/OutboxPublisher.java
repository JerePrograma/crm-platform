package com.gestudio.crm.outbox;

import com.gestudio.crm.common.OptimisticConflictException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxPublisher {

  private static final Pattern TYPE = Pattern.compile("[A-Z][A-Z0-9_]{2,99}");
  private static final Pattern AGGREGATE = Pattern.compile("[A-Z][A-Z0-9_]{1,49}");

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final OutboxProperties properties;

  public OutboxPublisher(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      Clock clock,
      OutboxProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.properties = properties;
  }

  public PublishedEvent publish(PublishCommand command) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException("Outbox publication requires an active transaction");
    }
    validate(command);
    String payload = json(canonical(command.payload()));
    if (payload.getBytes(StandardCharsets.UTF_8).length > properties.maxPayloadBytes()) {
      throw new IllegalArgumentException("Outbox payload exceeds configured limit");
    }
    String requestHash =
        sha256(
            command.eventType()
                + "\n"
                + command.eventVersion()
                + "\n"
                + command.aggregateType()
                + "\n"
                + command.aggregateId()
                + "\n"
                + payload);
    UUID id = UUID.randomUUID();
    Instant now = clock.instant();
    Instant availableAt = command.availableAt() == null ? now : command.availableAt();
    int inserted =
        jdbcTemplate.update(
            """
            INSERT INTO outbox_event (
              id, organization_id, event_type, event_version, aggregate_type, aggregate_id,
              payload, request_hash, status, attempt_count, max_attempts, next_attempt_at,
              created_at, updated_at, idempotency_key, correlation_id, created_by
            ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, 'PENDING', 0, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (organization_id, idempotency_key) DO NOTHING
            """,
            id,
            command.organizationId(),
            command.eventType(),
            command.eventVersion(),
            command.aggregateType(),
            command.aggregateId(),
            payload,
            requestHash,
            command.maxAttempts() == null ? properties.defaultMaxAttempts() : command.maxAttempts(),
            Timestamp.from(availableAt),
            Timestamp.from(now),
            Timestamp.from(now),
            command.idempotencyKey(),
            command.correlationId(),
            command.createdBy());
    if (inserted == 1) {
      return new PublishedEvent(id, OutboxStatus.PENDING, true, requestHash);
    }
    PublishedEvent existing =
        jdbcTemplate.queryForObject(
            """
            SELECT id, status, request_hash
            FROM outbox_event
            WHERE organization_id = ? AND idempotency_key = ?
            """,
            (rs, rowNum) ->
                new PublishedEvent(
                    rs.getObject("id", UUID.class),
                    OutboxStatus.valueOf(rs.getString("status")),
                    false,
                    rs.getString("request_hash")),
            command.organizationId(),
            command.idempotencyKey());
    if (!MessageDigest.isEqual(
        requestHash.getBytes(StandardCharsets.US_ASCII),
        existing.requestHash().getBytes(StandardCharsets.US_ASCII))) {
      throw new OptimisticConflictException(
          "Idempotency key was already used with a different request");
    }
    return existing;
  }

  private void validate(PublishCommand command) {
    if (command == null
        || command.organizationId() == null
        || command.eventType() == null
        || command.aggregateType() == null
        || command.aggregateId() == null
        || command.payload() == null) {
      throw new IllegalArgumentException(
          "Outbox organization, event, aggregate and payload are required");
    }
    if (!TYPE.matcher(command.eventType()).matches()
        || command.eventVersion() < 1
        || !AGGREGATE.matcher(command.aggregateType()).matches()) {
      throw new IllegalArgumentException("Outbox event or aggregate type is invalid");
    }
    if (command.idempotencyKey() == null
        || command.idempotencyKey().isBlank()
        || command.idempotencyKey().length() > 200) {
      throw new IllegalArgumentException("Outbox idempotency key is required");
    }
    if (command.correlationId() == null
        || command.correlationId().isBlank()
        || command.correlationId().length() > 128) {
      throw new IllegalArgumentException("Outbox correlation ID is required");
    }
    if (command.maxAttempts() != null
        && (command.maxAttempts() < 1 || command.maxAttempts() > 20)) {
      throw new IllegalArgumentException("Outbox maximum attempts must be between 1 and 20");
    }
  }

  private Object canonical(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> sorted = new TreeMap<>();
      map.forEach((key, child) -> sorted.put(String.valueOf(key), canonical(child)));
      return sorted;
    }
    if (value instanceof Iterable<?> iterable) {
      List<Object> sortedChildren = new ArrayList<>();
      iterable.forEach(child -> sortedChildren.add(canonical(child)));
      return sortedChildren;
    }
    return value;
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Outbox payload could not be serialized", exception);
    }
  }

  public static String sha256(String value) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  public record PublishCommand(
      UUID organizationId,
      String eventType,
      int eventVersion,
      String aggregateType,
      UUID aggregateId,
      Map<String, ?> payload,
      String idempotencyKey,
      String correlationId,
      UUID createdBy,
      Integer maxAttempts,
      Instant availableAt) {
    public PublishCommand(
        UUID organizationId,
        String eventType,
        int eventVersion,
        String aggregateType,
        UUID aggregateId,
        Map<String, ?> payload,
        String idempotencyKey,
        String correlationId,
        UUID createdBy,
        Integer maxAttempts) {
      this(
          organizationId,
          eventType,
          eventVersion,
          aggregateType,
          aggregateId,
          payload,
          idempotencyKey,
          correlationId,
          createdBy,
          maxAttempts,
          null);
    }
  }

  public record PublishedEvent(UUID id, OutboxStatus status, boolean created, String requestHash) {}
}
