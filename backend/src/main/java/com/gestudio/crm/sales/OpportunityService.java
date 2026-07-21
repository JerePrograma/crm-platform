package com.gestudio.crm.sales;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.common.UnprocessableEntityException;
import com.gestudio.crm.security.CurrentActor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityService {

  private static final Map<OpportunityStage, Set<OpportunityStage>> TRANSITIONS =
      Map.of(
          OpportunityStage.QUALIFICATION,
              EnumSet.of(OpportunityStage.DISCOVERY, OpportunityStage.LOST),
          OpportunityStage.DISCOVERY,
              EnumSet.of(
                  OpportunityStage.QUALIFICATION,
                  OpportunityStage.DEMO,
                  OpportunityStage.PROPOSAL,
                  OpportunityStage.LOST),
          OpportunityStage.DEMO,
              EnumSet.of(
                  OpportunityStage.DISCOVERY, OpportunityStage.PROPOSAL, OpportunityStage.LOST),
          OpportunityStage.PROPOSAL,
              EnumSet.of(
                  OpportunityStage.DEMO,
                  OpportunityStage.NEGOTIATION,
                  OpportunityStage.WON,
                  OpportunityStage.LOST),
          OpportunityStage.NEGOTIATION,
              EnumSet.of(OpportunityStage.PROPOSAL, OpportunityStage.WON, OpportunityStage.LOST),
          OpportunityStage.WON, EnumSet.noneOf(OpportunityStage.class),
          OpportunityStage.LOST, EnumSet.noneOf(OpportunityStage.class));

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final AuditEventWriter auditEventWriter;

  public OpportunityService(
      JdbcTemplate jdbcTemplate, CurrentActor currentActor, AuditEventWriter auditEventWriter) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.auditEventWriter = auditEventWriter;
  }

  @Transactional(readOnly = true)
  public OpportunityView get(UUID id) {
    return jdbcTemplate
        .query(
            select() + " WHERE o.organization_id = ? AND o.id = ?",
            this::view,
            currentActor.organizationId(),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + id));
  }

  @Transactional(readOnly = true)
  public List<OpportunityView> list(UUID prospectId, UUID ownerId, OpportunityStage stage) {
    List<Object> parameters = new ArrayList<>();
    parameters.add(currentActor.organizationId());
    StringBuilder where = new StringBuilder(" WHERE o.organization_id = ?");
    if (prospectId != null) {
      where.append(" AND o.prospect_id = ?");
      parameters.add(prospectId);
    }
    if (ownerId != null) {
      where.append(" AND o.owner_user_id = ?");
      parameters.add(ownerId);
    }
    if (stage != null) {
      where.append(" AND o.stage = ?");
      parameters.add(stage.name());
    }
    return jdbcTemplate.query(
        select() + where + " ORDER BY o.stage_changed_at DESC, o.id DESC",
        this::view,
        parameters.toArray());
  }

  @Transactional
  public OpportunityView create(CreateOpportunityCommand command) {
    requireProspect(command.prospectId());
    requireOwner(command.ownerId());
    validate(command.name(), command.estimatedValue(), command.currency(), command.probability());
    UUID id = UUID.randomUUID();
    boolean primary = command.primaryActive() || !hasActiveOpportunity(command.prospectId());
    if (primary) {
      clearPrimary(command.prospectId());
    }
    jdbcTemplate.update(
        """
        INSERT INTO opportunity (
          id, version, organization_id, prospect_id, name, owner_user_id, stage,
          estimated_value, currency, probability, expected_close_date, source,
          primary_active, stage_changed_at, created_by, updated_by, created_at, updated_at
        ) VALUES (?, 0, ?, ?, ?, ?, 'QUALIFICATION', ?, ?, ?, ?, ?, ?, now(), ?, ?, now(), now())
        """,
        id,
        currentActor.organizationId(),
        command.prospectId(),
        required(command.name(), "Name"),
        command.ownerId(),
        amount(command.estimatedValue()),
        currency(command.currency()),
        command.probability(),
        command.expectedCloseDate(),
        trim(command.source()),
        primary,
        currentActor.userIdOrNull(),
        currentActor.userIdOrNull());
    history(id, null, OpportunityStage.QUALIFICATION, null, "Opportunity created");
    activity(command.prospectId(), id, "Opportunity created", OpportunityStage.QUALIFICATION);
    audit("OPPORTUNITY_CREATED", id, OpportunityStage.QUALIFICATION, null);
    return get(id);
  }

  @Transactional
  public OpportunityView update(UUID id, UpdateOpportunityCommand command) {
    OpportunityView before = get(id);
    if (before.stage().terminal()) {
      throw new OptimisticConflictException("Closed opportunities cannot be edited");
    }
    requireOwner(command.ownerId());
    validate(command.name(), command.estimatedValue(), command.currency(), command.probability());
    if (command.primaryActive()) {
      jdbcTemplate.update(
          "UPDATE opportunity SET primary_active = FALSE, updated_at = now(), version = version + 1 WHERE organization_id = ? AND prospect_id = ? AND primary_active AND id <> ?",
          currentActor.organizationId(),
          before.prospectId(),
          id);
    }
    int updated =
        jdbcTemplate.update(
            """
            UPDATE opportunity SET name = ?, owner_user_id = ?, estimated_value = ?, currency = ?,
              probability = ?, expected_close_date = ?, source = ?, primary_active = ?,
              updated_by = ?, updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            required(command.name(), "Name"),
            command.ownerId(),
            amount(command.estimatedValue()),
            currency(command.currency()),
            command.probability(),
            command.expectedCloseDate(),
            trim(command.source()),
            command.primaryActive(),
            currentActor.userIdOrNull(),
            id,
            currentActor.organizationId(),
            command.version());
    if (updated == 0) {
      requireExistsOrConflict(id, command.version());
    }
    audit("OPPORTUNITY_UPDATED", id, before.stage(), null);
    return get(id);
  }

  @Transactional
  public OpportunityView transition(UUID id, TransitionCommand command) {
    OpportunityView before = get(id);
    if (!TRANSITIONS.get(before.stage()).contains(command.stage())) {
      throw new UnprocessableEntityException(
          "Transition " + before.stage() + " -> " + command.stage() + " is not allowed");
    }
    String lostReason =
        command.stage() == OpportunityStage.LOST ? required(command.reason(), "Lost reason") : null;
    String wonReason =
        command.stage() == OpportunityStage.WON ? required(command.reason(), "Won reason") : null;
    int probability =
        command.stage() == OpportunityStage.WON
            ? 100
            : command.stage() == OpportunityStage.LOST ? 0 : defaultProbability(command.stage());
    int updated =
        jdbcTemplate.update(
            """
            UPDATE opportunity SET stage = ?, probability = ?, actual_close_date = ?,
              lost_reason = ?, won_reason = ?, primary_active = CASE WHEN ? THEN FALSE ELSE primary_active END,
              stage_changed_at = now(), updated_by = ?, updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            command.stage().name(),
            probability,
            command.stage().terminal() ? LocalDate.now() : null,
            lostReason,
            wonReason,
            command.stage().terminal(),
            currentActor.userIdOrNull(),
            id,
            currentActor.organizationId(),
            command.version());
    if (updated == 0) {
      requireExistsOrConflict(id, command.version());
    }
    history(id, before.stage(), command.stage(), command.reason(), command.comment());
    activity(before.prospectId(), id, "Opportunity moved to " + command.stage(), command.stage());
    if (command.stage() == OpportunityStage.WON) {
      convertProspectToCustomer(before.prospectId(), id);
    }
    audit("OPPORTUNITY_STAGE_CHANGED", id, command.stage(), before.stage());
    return get(id);
  }

  @Transactional(readOnly = true)
  public PipelineMetrics metrics() {
    Map<OpportunityStage, Long> byStage = new EnumMap<>(OpportunityStage.class);
    for (OpportunityStage stage : OpportunityStage.values()) {
      byStage.put(stage, 0L);
    }
    jdbcTemplate
        .queryForList(
            "SELECT stage, count(*) AS total FROM opportunity WHERE organization_id = ? GROUP BY stage",
            currentActor.organizationId())
        .forEach(
            row ->
                byStage.put(
                    OpportunityStage.valueOf((String) row.get("stage")),
                    ((Number) row.get("total")).longValue()));
    Map<String, Object> totals =
        jdbcTemplate.queryForMap(
            """
            SELECT count(*) AS active_count,
              coalesce(sum(estimated_value), 0) AS total_value,
              coalesce(sum(estimated_value * probability / 100.0), 0) AS weighted_value,
              count(*) FILTER (WHERE stage_changed_at < now() - interval '30 days') AS stalled_count
            FROM opportunity WHERE organization_id = ? AND stage NOT IN ('WON', 'LOST')
            """,
            currentActor.organizationId());
    return new PipelineMetrics(
        ((Number) totals.get("active_count")).longValue(),
        (BigDecimal) totals.get("total_value"),
        ((BigDecimal) totals.get("weighted_value")).setScale(2, RoundingMode.HALF_UP),
        ((Number) totals.get("stalled_count")).longValue(),
        byStage);
  }

  @Transactional(readOnly = true)
  public List<StageHistoryView> history(UUID opportunityId) {
    get(opportunityId);
    return jdbcTemplate.query(
        """
        SELECT id, previous_stage, new_stage, reason, comment, created_at
        FROM opportunity_stage_history
        WHERE organization_id = ? AND opportunity_id = ?
        ORDER BY created_at DESC, id DESC
        """,
        (resultSet, rowNumber) ->
            new StageHistoryView(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("previous_stage") == null
                    ? null
                    : OpportunityStage.valueOf(resultSet.getString("previous_stage")),
                OpportunityStage.valueOf(resultSet.getString("new_stage")),
                resultSet.getString("reason"),
                resultSet.getString("comment"),
                resultSet.getTimestamp("created_at").toInstant()),
        currentActor.organizationId(),
        opportunityId);
  }

  private void convertProspectToCustomer(UUID prospectId, UUID opportunityId) {
    Map<String, Object> prospect =
        jdbcTemplate.queryForMap(
            "SELECT status FROM prospect WHERE id = ? AND organization_id = ? FOR UPDATE",
            prospectId,
            currentActor.organizationId());
    String previous = (String) prospect.get("status");
    if (!"CUSTOMER".equals(previous)) {
      jdbcTemplate.update(
          "UPDATE prospect SET status = 'CUSTOMER', status_detail_at = now(), updated_by = ?, updated_at = now(), version = version + 1 WHERE id = ? AND organization_id = ?",
          currentActor.userIdOrNull(),
          prospectId,
          currentActor.organizationId());
      jdbcTemplate.update(
          "INSERT INTO prospect_status_history (id, organization_id, prospect_id, actor_user_id, previous_status, new_status, reason, comment, source, created_at) VALUES (?, ?, ?, ?, ?, 'CUSTOMER', 'OPPORTUNITY_WON', ?, 'OPPORTUNITY', now())",
          UUID.randomUUID(),
          currentActor.organizationId(),
          prospectId,
          currentActor.userIdOrNull(),
          previous,
          "Opportunity " + opportunityId + " won");
    }
    jdbcTemplate.update(
        """
        UPDATE crm_task SET status = 'CANCELLED', cancelled_at = now(), completed_at = NULL,
          outcome = 'Cancelled after opportunity won', updated_at = now(), version = version + 1
        WHERE organization_id = ? AND prospect_id = ? AND status IN ('OPEN', 'IN_PROGRESS')
          AND task_type IN ('PROSPECTING', 'FOLLOW_UP')
        """,
        currentActor.organizationId(),
        prospectId);
    jdbcTemplate.update(
        """
        UPDATE prospect SET next_action_at = (
          SELECT min(due_at) FROM crm_task
          WHERE organization_id = ? AND prospect_id = ? AND status IN ('OPEN', 'IN_PROGRESS')
        ), updated_at = now(), version = version + 1
        WHERE organization_id = ? AND id = ?
        """,
        currentActor.organizationId(),
        prospectId,
        currentActor.organizationId(),
        prospectId);
  }

  private void activity(
      UUID prospectId, UUID opportunityId, String summary, OpportunityStage stage) {
    jdbcTemplate.update(
        """
        INSERT INTO activity (
          id, organization_id, prospect_id, actor_user_id, activity_type, occurred_at,
          direction, outcome, summary, external_reference, metadata, created_at
        ) VALUES (?, ?, ?, ?, 'CUSTOM', now(), 'INTERNAL', ?, ?, ?, jsonb_build_object('opportunityId', ?::text, 'stage', ?), now())
        """,
        UUID.randomUUID(),
        currentActor.organizationId(),
        prospectId,
        currentActor.userIdOrNull(),
        stage.name(),
        summary,
        "opportunity:" + opportunityId + ":" + stage.name() + ":" + UUID.randomUUID(),
        opportunityId,
        stage.name());
  }

  private void history(
      UUID opportunityId,
      OpportunityStage previous,
      OpportunityStage next,
      String reason,
      String comment) {
    jdbcTemplate.update(
        "INSERT INTO opportunity_stage_history (id, organization_id, opportunity_id, actor_user_id, previous_stage, new_stage, reason, comment, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())",
        UUID.randomUUID(),
        currentActor.organizationId(),
        opportunityId,
        currentActor.userIdOrNull(),
        previous == null ? null : previous.name(),
        next.name(),
        trim(reason),
        trim(comment));
  }

  private void audit(
      String action, UUID opportunityId, OpportunityStage stage, OpportunityStage previous) {
    LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
    payload.put("stage", stage.name());
    payload.put("previousStage", previous == null ? null : previous.name());
    auditEventWriter.record(action, "Opportunity", opportunityId, payload);
  }

  private void requireProspect(UUID prospectId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM prospect WHERE id = ? AND organization_id = ? AND merged_into_id IS NULL AND archived_at IS NULL",
            Integer.class,
            prospectId,
            currentActor.organizationId());
    if (count == null || count != 1) {
      throw new ResourceNotFoundException("Prospect not found in the current organization");
    }
  }

  private void requireOwner(UUID ownerId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM organization_membership membership
            JOIN app_user app_user ON app_user.id = membership.user_id
            WHERE membership.organization_id = ? AND membership.user_id = ? AND app_user.active
            """,
            Integer.class,
            currentActor.organizationId(),
            ownerId);
    if (count == null || count != 1) {
      throw new UnprocessableEntityException("Owner must be an active organization member");
    }
  }

  private void requireExistsOrConflict(UUID id, long expectedVersion) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM opportunity WHERE id = ? AND organization_id = ?",
            Integer.class,
            id,
            currentActor.organizationId());
    if (count == null || count == 0) {
      throw new ResourceNotFoundException("Opportunity not found: " + id);
    }
    throw new OptimisticConflictException(
        "Opportunity was modified; expected version " + expectedVersion);
  }

  private boolean hasActiveOpportunity(UUID prospectId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM opportunity WHERE organization_id = ? AND prospect_id = ? AND stage NOT IN ('WON', 'LOST')",
            Integer.class,
            currentActor.organizationId(),
            prospectId);
    return count != null && count > 0;
  }

  private void clearPrimary(UUID prospectId) {
    jdbcTemplate.update(
        "UPDATE opportunity SET primary_active = FALSE, updated_at = now(), version = version + 1 WHERE organization_id = ? AND prospect_id = ? AND primary_active",
        currentActor.organizationId(),
        prospectId);
  }

  private void validate(String name, BigDecimal value, String currency, int probability) {
    required(name, "Name");
    if (value == null || value.signum() < 0) {
      throw new IllegalArgumentException("Estimated value must be zero or positive");
    }
    currency(currency);
    if (probability < 0 || probability > 100) {
      throw new IllegalArgumentException("Probability must be between 0 and 100");
    }
  }

  private String currency(String value) {
    String result = required(value, "Currency").toUpperCase(java.util.Locale.ROOT);
    if (!result.matches("[A-Z]{3}")) {
      throw new IllegalArgumentException("Currency must be a three-letter ISO code");
    }
    String organizationCurrency =
        jdbcTemplate.queryForObject(
            "SELECT currency FROM organization WHERE id = ?",
            String.class,
            currentActor.organizationId());
    if (!result.equals(organizationCurrency)) {
      throw new UnprocessableEntityException(
          "Opportunity currency must match the organization currency " + organizationCurrency);
    }
    return result;
  }

  private BigDecimal amount(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private int defaultProbability(OpportunityStage stage) {
    return switch (stage) {
      case QUALIFICATION -> 10;
      case DISCOVERY -> 25;
      case DEMO -> 45;
      case PROPOSAL -> 65;
      case NEGOTIATION -> 80;
      case WON -> 100;
      case LOST -> 0;
    };
  }

  private String required(String value, String label) {
    String result = trim(value);
    if (result == null) {
      throw new IllegalArgumentException(label + " is required");
    }
    return result;
  }

  private String trim(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String select() {
    return """
        SELECT o.id, o.version, o.prospect_id, i.name AS prospect_name, o.name, o.owner_user_id,
          u.display_name AS owner_name, o.stage, o.estimated_value, o.currency, o.probability,
          o.expected_close_date, o.actual_close_date, o.lost_reason, o.won_reason, o.source,
          o.primary_active, o.stage_changed_at, o.created_at, o.updated_at
        FROM opportunity o JOIN prospect p ON p.id = o.prospect_id AND p.organization_id = o.organization_id
        JOIN institution i ON i.id = p.institution_id AND i.organization_id = p.organization_id
        JOIN app_user u ON u.id = o.owner_user_id
        """;
  }

  private OpportunityView view(ResultSet resultSet, int rowNumber) throws SQLException {
    return new OpportunityView(
        resultSet.getObject("id", UUID.class),
        resultSet.getLong("version"),
        resultSet.getObject("prospect_id", UUID.class),
        resultSet.getString("prospect_name"),
        resultSet.getString("name"),
        resultSet.getObject("owner_user_id", UUID.class),
        resultSet.getString("owner_name"),
        OpportunityStage.valueOf(resultSet.getString("stage")),
        resultSet.getBigDecimal("estimated_value"),
        resultSet.getString("currency"),
        resultSet.getInt("probability"),
        resultSet.getObject("expected_close_date", LocalDate.class),
        resultSet.getObject("actual_close_date", LocalDate.class),
        resultSet.getString("lost_reason"),
        resultSet.getString("won_reason"),
        resultSet.getString("source"),
        resultSet.getBoolean("primary_active"),
        resultSet.getTimestamp("stage_changed_at").toInstant(),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("updated_at").toInstant());
  }

  public record CreateOpportunityCommand(
      UUID prospectId,
      String name,
      UUID ownerId,
      BigDecimal estimatedValue,
      String currency,
      int probability,
      LocalDate expectedCloseDate,
      String source,
      boolean primaryActive) {}

  public record UpdateOpportunityCommand(
      long version,
      String name,
      UUID ownerId,
      BigDecimal estimatedValue,
      String currency,
      int probability,
      LocalDate expectedCloseDate,
      String source,
      boolean primaryActive) {}

  public record TransitionCommand(
      long version, OpportunityStage stage, String reason, String comment) {}

  public record OpportunityView(
      UUID id,
      long version,
      UUID prospectId,
      String prospectName,
      String name,
      UUID ownerId,
      String ownerName,
      OpportunityStage stage,
      BigDecimal estimatedValue,
      String currency,
      int probability,
      LocalDate expectedCloseDate,
      LocalDate actualCloseDate,
      String lostReason,
      String wonReason,
      String source,
      boolean primaryActive,
      Instant stageChangedAt,
      Instant createdAt,
      Instant updatedAt) {}

  public record StageHistoryView(
      UUID id,
      OpportunityStage previousStage,
      OpportunityStage newStage,
      String reason,
      String comment,
      Instant createdAt) {}

  public record PipelineMetrics(
      long activeCount,
      BigDecimal totalValue,
      BigDecimal weightedValue,
      long stalledCount,
      Map<OpportunityStage, Long> byStage) {}
}
