package com.gestudio.crm.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OutboxWorkerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxWorkerService.class);

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;
  private final OutboxEventProcessor processor;
  private final OutboxProperties properties;
  private final Clock clock;
  private final MeterRegistry meterRegistry;
  private final String workerId;
  private final AtomicBoolean acceptingWork = new AtomicBoolean(true);
  private final AtomicBoolean running = new AtomicBoolean(false);

  public OutboxWorkerService(
      JdbcTemplate jdbcTemplate,
      TransactionTemplate transactionTemplate,
      OutboxEventProcessor processor,
      OutboxProperties properties,
      Clock clock,
      MeterRegistry meterRegistry) {
    this.jdbcTemplate = jdbcTemplate;
    this.transactionTemplate = transactionTemplate;
    this.processor = processor;
    this.properties = properties;
    this.clock = clock;
    this.meterRegistry = meterRegistry;
    this.workerId = ManagementFactory.getRuntimeMXBean().getName() + "-" + UUID.randomUUID();
  }

  @Scheduled(
      fixedDelayString = "${crm.outbox.poll-delay:5s}",
      initialDelayString = "${crm.outbox.initial-delay:5s}")
  public void scheduledRun() {
    if (properties.enabled()) {
      runOnce();
    }
  }

  public RunResult runOnce() {
    if (!acceptingWork.get() || !running.compareAndSet(false, true)) {
      return new RunResult(workerId, 0, 0, 0, false);
    }
    int recovered = 0;
    int claimed = 0;
    int completed = 0;
    try {
      recovered = recoverExpiredLeases();
      List<ClaimedEvent> events = claimBatch();
      claimed = events.size();
      for (ClaimedEvent event : events) {
        if (!acceptingWork.get()) {
          break;
        }
        processOne(event);
        completed++;
      }
      return new RunResult(workerId, recovered, claimed, completed, true);
    } finally {
      running.set(false);
    }
  }

  private int recoverExpiredLeases() {
    Instant now = clock.instant();
    Integer recovered =
        transactionTemplate.execute(
            status -> {
              int dead =
                  jdbcTemplate.update(
                      """
                      UPDATE outbox_event SET status = 'DEAD', processed_at = ?, updated_at = ?,
                        locked_at = NULL, lock_expires_at = NULL, locked_by = NULL,
                        last_error_code = 'LEASE_EXPIRED',
                        last_error_summary = 'Worker lease expired at maximum attempts'
                      WHERE status = 'PROCESSING' AND lock_expires_at <= ?
                        AND attempt_count >= max_attempts
                      """,
                      Timestamp.from(now),
                      Timestamp.from(now),
                      Timestamp.from(now));
              int retry =
                  jdbcTemplate.update(
                      """
                      UPDATE outbox_event SET status = 'RETRY', next_attempt_at = ?, updated_at = ?,
                        locked_at = NULL, lock_expires_at = NULL, locked_by = NULL,
                        last_error_code = 'LEASE_EXPIRED',
                        last_error_summary = 'Worker lease expired and was recovered'
                      WHERE status = 'PROCESSING' AND lock_expires_at <= ?
                        AND attempt_count < max_attempts
                      """,
                      Timestamp.from(now),
                      Timestamp.from(now),
                      Timestamp.from(now));
              return dead + retry;
            });
    return recovered == null ? 0 : recovered;
  }

  private List<ClaimedEvent> claimBatch() {
    Instant now = clock.instant();
    Instant expires = now.plus(properties.lease());
    List<ClaimedEvent> claimed =
        transactionTemplate.execute(
            status ->
                jdbcTemplate.query(
                    """
                    WITH candidates AS (
                      SELECT e.id
                      FROM outbox_event e
                      WHERE e.status IN ('PENDING', 'RETRY') AND e.next_attempt_at <= ?
                        AND NOT EXISTS (
                          SELECT 1 FROM system_setting s
                          WHERE s.organization_id = e.organization_id
                            AND s.setting_key = 'outbox-worker-paused'
                            AND lower(s.setting_value) = 'true'
                        )
                      ORDER BY e.next_attempt_at, e.created_at, e.id
                      FOR UPDATE SKIP LOCKED
                      LIMIT ?
                    )
                    UPDATE outbox_event e
                    SET status = 'PROCESSING', attempt_count = attempt_count + 1,
                      locked_at = ?, lock_expires_at = ?, locked_by = ?, updated_at = ?
                    FROM candidates c
                    WHERE e.id = c.id
                    RETURNING e.id, e.organization_id, e.event_type, e.event_version,
                      e.aggregate_type, e.aggregate_id, e.payload::text, e.attempt_count,
                      e.max_attempts, e.correlation_id
                    """,
                    this::claimedEvent,
                    Timestamp.from(now),
                    properties.batchSize(),
                    Timestamp.from(now),
                    Timestamp.from(expires),
                    workerId,
                    Timestamp.from(now)));
    return claimed == null ? List.of() : claimed;
  }

  private void processOne(ClaimedEvent event) {
    OutboxProcessingResult result;
    try {
      result = processor.process(event);
    } catch (IllegalArgumentException exception) {
      result =
          OutboxProcessingResult.failure(
              OutboxErrorCategory.NON_RETRYABLE, "INVALID_PAYLOAD", safe(exception.getMessage()));
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "outbox processing failed eventId={} organizationId={} correlationId={} code=INTERNAL_TRANSIENT",
          event.id(),
          event.organizationId(),
          event.correlationId());
      result =
          OutboxProcessingResult.failure(
              OutboxErrorCategory.RETRYABLE,
              "INTERNAL_TRANSIENT",
              "Transient internal processing failure");
    }
    finalizeEvent(event, result);
  }

  private void finalizeEvent(ClaimedEvent event, OutboxProcessingResult result) {
    Instant now = clock.instant();
    OutboxStatus target;
    Instant next = now;
    if (result.succeeded()) {
      target = OutboxStatus.SUCCEEDED;
    } else {
      target =
          switch (result.category()) {
            case RETRYABLE ->
                event.attemptCount() >= event.maxAttempts()
                    ? OutboxStatus.DEAD
                    : OutboxStatus.RETRY;
            case POLICY_BLOCK, CONFIGURATION_BLOCK -> OutboxStatus.BLOCKED;
            case CANCELLED, DUPLICATE -> OutboxStatus.CANCELLED;
            case NON_RETRYABLE -> OutboxStatus.DEAD;
          };
      if (target == OutboxStatus.RETRY) {
        next = now.plus(backoff(event.id(), event.attemptCount()));
      }
    }
    Instant processedAt =
        target == OutboxStatus.RETRY || target == OutboxStatus.PENDING ? null : now;
    int updated =
        jdbcTemplate.update(
            """
            UPDATE outbox_event SET status = ?, next_attempt_at = ?, updated_at = ?,
              processed_at = ?, locked_at = NULL, lock_expires_at = NULL, locked_by = NULL,
              last_error_code = ?, last_error_summary = ?, result_summary = ?
            WHERE id = ? AND organization_id = ? AND status = 'PROCESSING' AND locked_by = ?
            """,
            target.name(),
            Timestamp.from(next),
            Timestamp.from(now),
            processedAt == null ? null : Timestamp.from(processedAt),
            result.code(),
            safe(result.summary()),
            safe(result.resultSummary()),
            event.id(),
            event.organizationId(),
            workerId);
    if (updated != 1) {
      throw new IllegalStateException("Outbox compare-and-set failed for event " + event.id());
    }
    meterRegistry.counter("crm.outbox.processed", "status", target.name()).increment();
  }

  Duration backoff(UUID eventId, int attempt) {
    long base = properties.baseBackoff().toMillis();
    long cap = properties.maxBackoff().toMillis();
    int exponent = Math.max(0, Math.min(attempt - 1, 20));
    long exponential = base > (Long.MAX_VALUE >> exponent) ? Long.MAX_VALUE : base << exponent;
    long jitter = base == 0 ? 0 : Math.floorMod(eventId.getLeastSignificantBits(), base + 1);
    return Duration.ofMillis(
        Math.min(cap, Math.min(Long.MAX_VALUE - jitter, exponential) + jitter));
  }

  private ClaimedEvent claimedEvent(ResultSet rs, int rowNum) throws SQLException {
    return new ClaimedEvent(
        rs.getObject("id", UUID.class),
        rs.getObject("organization_id", UUID.class),
        rs.getString("event_type"),
        rs.getInt("event_version"),
        rs.getString("aggregate_type"),
        rs.getObject("aggregate_id", UUID.class),
        rs.getString("payload"),
        rs.getInt("attempt_count"),
        rs.getInt("max_attempts"),
        rs.getString("correlation_id"));
  }

  private String safe(String value) {
    if (value == null) {
      return null;
    }
    String singleLine = value.replaceAll("[\\r\\n\\t]+", " ").trim();
    return singleLine.substring(0, Math.min(singleLine.length(), 500));
  }

  public WorkerHealth health() {
    return new WorkerHealth(
        workerId, properties.enabled(), acceptingWork.get(), running.get(), properties.batchSize());
  }

  @PreDestroy
  void stop() {
    acceptingWork.set(false);
  }

  public record ClaimedEvent(
      UUID id,
      UUID organizationId,
      String eventType,
      int eventVersion,
      String aggregateType,
      UUID aggregateId,
      String payload,
      int attemptCount,
      int maxAttempts,
      String correlationId) {}

  public record RunResult(
      String workerId, int recovered, int claimed, int completed, boolean executed) {}

  public record WorkerHealth(
      String workerId,
      boolean schedulerEnabled,
      boolean acceptingWork,
      boolean running,
      int batchSize) {}
}
