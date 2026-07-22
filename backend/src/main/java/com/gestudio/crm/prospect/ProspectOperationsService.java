package com.gestudio.crm.prospect;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.CsvSafety;
import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.common.UnprocessableEntityException;
import com.gestudio.crm.security.CurrentActor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProspectOperationsService {

  private static final Set<String> LOSS_REASONS =
      Set.of(
          "NO_RESPONSE",
          "NO_INTEREST",
          "NO_BUDGET",
          "USES_COMPETITOR",
          "BAD_TIMING",
          "NOT_A_FIT",
          "INVALID_DATA",
          "CLOSED",
          "OTHER");
  private static final Map<String, String> SORT_COLUMNS =
      Map.of(
          "createdAt", "p.created_at",
          "updatedAt", "p.updated_at",
          "displayName", "i.name",
          "status", "p.status",
          "priority", "p.priority",
          "score", "p.score",
          "nextActionAt", "p.next_action_at");

  private final JdbcTemplate jdbcTemplate;
  private final NormalizationService normalizationService;
  private final ProspectLifecycle lifecycle;
  private final CurrentActor currentActor;
  private final AuditEventWriter auditEventWriter;

  public ProspectOperationsService(
      JdbcTemplate jdbcTemplate,
      NormalizationService normalizationService,
      ProspectLifecycle lifecycle,
      CurrentActor currentActor,
      AuditEventWriter auditEventWriter) {
    this.jdbcTemplate = jdbcTemplate;
    this.normalizationService = normalizationService;
    this.lifecycle = lifecycle;
    this.currentActor = currentActor;
    this.auditEventWriter = auditEventWriter;
  }

  @Transactional(readOnly = true)
  public OperationalProspectView get(UUID id) {
    return jdbcTemplate
        .query(
            baseSelect() + " WHERE p.organization_id = ? AND p.id = ?",
            this::prospect,
            currentActor.organizationId(),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + id));
  }

  @Transactional(readOnly = true)
  public PageResult<OperationalProspectView> search(SearchFilter filter) {
    int page = Math.max(0, filter.page());
    int size = Math.min(Math.max(filter.size(), 1), 200);
    List<Object> parameters = new ArrayList<>();
    String where = where(filter, parameters);
    String sortColumn = SORT_COLUMNS.getOrDefault(filter.sort(), "p.created_at");
    String direction = "asc".equalsIgnoreCase(filter.direction()) ? "ASC" : "DESC";
    List<Object> pageParameters = new ArrayList<>(parameters);
    String ordering;
    if (filter.query() != null
        && !filter.query().isBlank()
        && "relevance".equalsIgnoreCase(filter.sort())) {
      String normalized = filter.query().trim().toLowerCase(Locale.ROOT);
      ordering =
          "CASE WHEN lower(i.name) = ? THEN 0 WHEN lower(i.name) LIKE ? ESCAPE '\\' THEN 1 ELSE 2 END ASC, "
              + "similarity(lower(i.name), ?) DESC, p.updated_at DESC, p.id ASC";
      pageParameters.add(normalized);
      pageParameters.add(escapedLike(normalized) + "%");
      pageParameters.add(normalized);
    } else {
      ordering = sortColumn + " " + direction + " NULLS LAST, p.id " + direction;
    }
    pageParameters.add(size);
    pageParameters.add(page * size);
    List<OperationalProspectView> content =
        jdbcTemplate.query(
            baseSelect() + where + " ORDER BY " + ordering + " LIMIT ? OFFSET ?",
            this::prospect,
            pageParameters.toArray());
    Long total =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM prospect p JOIN institution i ON i.id = p.institution_id "
                + where,
            Long.class,
            parameters.toArray());
    long count = total == null ? 0 : total;
    return new PageResult<>(content, count, (int) Math.ceil((double) count / size), page, size);
  }

  @Transactional
  public OperationalProspectView update(UUID id, UpdateProspectCommand command) {
    OperationalProspectView before = get(id);
    validate(command);
    if (!java.util.Objects.equals(before.ownerUserId(), command.ownerUserId())
        && !currentActor.requiredPrincipal().permissions().contains("PROSPECT_ASSIGN")) {
      throw new org.springframework.security.access.AccessDeniedException(
          "PROSPECT_ASSIGN is required to change ownership");
    }
    if (command.ownerUserId() != null && !validOwner(command.ownerUserId())) {
      throw new UnprocessableEntityException("Owner must be an active member of the organization");
    }
    int updated =
        jdbcTemplate.update(
            """
            UPDATE prospect
            SET priority = ?, score = ?, estimated_students = ?, source = ?, source_detail = ?,
                owner_user_id = ?, notes_summary = ?, next_action_at = ?, updated_by = ?,
                updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            command.priority(),
            command.score(),
            command.estimatedStudents(),
            trim(command.source()),
            trim(command.sourceDetail()),
            command.ownerUserId(),
            trim(command.notesSummary()),
            timestamp(command.nextActionAt()),
            currentActor.userIdOrNull(),
            id,
            currentActor.organizationId(),
            command.version());
    if (updated == 0) {
      requireExistsOrConflict(id, command.version());
    }
    jdbcTemplate.update(
        """
        UPDATE institution
        SET name = ?, normalized_name = ?, legal_name = ?, website = ?, website_domain = ?,
            address = ?, locality = ?, normalized_locality = ?, province = ?, country = ?,
            timezone = ?, updated_at = now(), version = version + 1
        WHERE id = ? AND organization_id = ?
        """,
        required(command.displayName(), "Display name"),
        normalizationService.normalizeName(command.displayName()),
        trim(command.legalName()),
        trim(command.website()),
        normalizationService.normalizeDomain(command.website()),
        trim(command.address()),
        trim(command.city()),
        normalizationService.normalizeText(command.city()),
        trim(command.province()),
        trim(command.country()),
        trim(command.timezone()),
        before.institutionId(),
        currentActor.organizationId());
    auditEventWriter.record(
        "PROSPECT_UPDATED",
        "Prospect",
        id,
        Map.of("previousVersion", command.version(), "displayName", command.displayName()));
    return get(id);
  }

  @Transactional
  public OperationalProspectView archive(UUID id, long version) {
    OperationalProspectView before = get(id);
    if (before.archivedAt() != null) {
      return before;
    }
    int updated =
        jdbcTemplate.update(
            """
            UPDATE prospect SET status = 'ARCHIVED', archived_at = now(), status_detail_at = now(),
              updated_by = ?, updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            currentActor.userIdOrNull(),
            id,
            currentActor.organizationId(),
            version);
    if (updated == 0) {
      requireExistsOrConflict(id, version);
    }
    history(id, before.status(), ProspectStatus.ARCHIVED, null, "Archived", "API");
    auditEventWriter.record("PROSPECT_ARCHIVED", "Prospect", id, Map.of());
    return get(id);
  }

  @Transactional
  public OperationalProspectView restore(UUID id, long version) {
    OperationalProspectView before = get(id);
    if (before.archivedAt() == null) {
      return before;
    }
    ProspectStatus restored =
        jdbcTemplate
            .query(
                """
                SELECT previous_status FROM prospect_status_history
                WHERE organization_id = ? AND prospect_id = ? AND new_status = 'ARCHIVED'
                ORDER BY created_at DESC, id DESC LIMIT 1
                """,
                (resultSet, rowNumber) -> ProspectStatus.valueOf(resultSet.getString(1)),
                currentActor.organizationId(),
                id)
            .stream()
            .findFirst()
            .orElse(ProspectStatus.NEW);
    int updated =
        jdbcTemplate.update(
            """
            UPDATE prospect SET archived_at = NULL, status = ?, updated_at = now(),
              updated_by = ?, version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            restored.name(),
            currentActor.userIdOrNull(),
            id,
            currentActor.organizationId(),
            version);
    if (updated == 0) {
      requireExistsOrConflict(id, version);
    }
    history(id, ProspectStatus.ARCHIVED, restored, null, "Restored", "API");
    auditEventWriter.record("PROSPECT_RESTORED", "Prospect", id, Map.of("status", restored.name()));
    return get(id);
  }

  @Transactional
  public OperationalProspectView transition(UUID id, TransitionCommand command) {
    OperationalProspectView before = get(id);
    if (before.archivedAt() != null) {
      throw new UnprocessableEntityException("Archived prospects must be restored first");
    }
    lifecycle.requireAllowed(before.status(), command.status());
    validateTransition(before, command);
    updateLifecycle(
        id, command.version(), command.status(), command.reason(), command.scheduledAt());
    history(
        id,
        before.status(),
        command.status(),
        trim(command.reason()),
        trim(command.comment()),
        "API");
    auditEventWriter.record(
        "PROSPECT_STATUS_CHANGED",
        "Prospect",
        id,
        Map.of("from", before.status().name(), "to", command.status().name()));
    return get(id);
  }

  @Transactional(readOnly = true)
  public String exportCsv(SearchFilter filter) {
    PageResult<OperationalProspectView> results =
        search(
            new SearchFilter(
                filter.query(),
                filter.status(),
                filter.ownerUserId(),
                filter.archived(),
                0,
                10000,
                filter.sort(),
                filter.direction()));
    StringBuilder csv =
        new StringBuilder(
            "id,display_name,status,eligibility,priority,score,city,province,country,owner,next_action_at\r\n");
    results
        .content()
        .forEach(
            prospect ->
                csv.append(csv(prospect.id().toString()))
                    .append(',')
                    .append(csv(prospect.displayName()))
                    .append(',')
                    .append(csv(prospect.status().name()))
                    .append(',')
                    .append(csv(prospect.eligibility().name()))
                    .append(',')
                    .append(csv(value(prospect.priority())))
                    .append(',')
                    .append(csv(value(prospect.score())))
                    .append(',')
                    .append(csv(prospect.city()))
                    .append(',')
                    .append(csv(prospect.province()))
                    .append(',')
                    .append(csv(prospect.country()))
                    .append(',')
                    .append(csv(prospect.ownerName()))
                    .append(',')
                    .append(csv(value(prospect.nextActionAt())))
                    .append("\r\n"));
    return csv.toString();
  }

  private String baseSelect() {
    return """
        SELECT p.id, p.version, p.institution_id, i.name, i.legal_name, p.status,
               p.eligibility, p.priority, p.score, p.estimated_students, p.source,
               p.source_detail, p.owner_user_id, u.display_name AS owner_name, i.website,
               i.address, i.locality, i.province, i.country, i.timezone, p.notes_summary,
               p.next_action_at, p.last_contact_at, p.contact_eligible, p.lost_reason,
               p.status_detail_at, p.archived_at, p.created_at, p.updated_at
        FROM prospect p
        JOIN institution i ON i.id = p.institution_id AND i.organization_id = p.organization_id
        LEFT JOIN app_user u ON u.id = p.owner_user_id
        """;
  }

  private String where(SearchFilter filter, List<Object> parameters) {
    StringBuilder where = new StringBuilder(" WHERE p.organization_id = ?");
    parameters.add(currentActor.organizationId());
    if (filter.status() != null) {
      where.append(" AND p.status = ?");
      parameters.add(filter.status().name());
    }
    if (filter.ownerUserId() != null) {
      where.append(" AND p.owner_user_id = ?");
      parameters.add(filter.ownerUserId());
    }
    where.append(
        filter.archived() ? " AND p.archived_at IS NOT NULL" : " AND p.archived_at IS NULL");
    if (filter.query() != null && !filter.query().isBlank()) {
      where.append(
          """
           AND (lower(i.name) LIKE ? ESCAPE '\\'
             OR lower(coalesce(i.legal_name, '')) LIKE ? ESCAPE '\\'
             OR lower(coalesce(i.locality, '')) LIKE ? ESCAPE '\\'
             OR lower(coalesce(i.province, '')) LIKE ? ESCAPE '\\'
             OR lower(coalesce(i.website, '')) LIKE ? ESCAPE '\\'
             OR lower(coalesce(p.notes_summary, '')) LIKE ? ESCAPE '\\'
             OR EXISTS (
               SELECT 1 FROM contact c LEFT JOIN contact_channel cc ON cc.contact_id = c.id
               WHERE c.institution_id = i.id AND c.organization_id = p.organization_id
                 AND c.deleted_at IS NULL
                 AND (lower(coalesce(c.name, '')) LIKE ? ESCAPE '\\'
                   OR lower(coalesce(cc.normalized_value, '')) LIKE ? ESCAPE '\\')
             )
             OR EXISTS (
               SELECT 1 FROM prospect_tag pt JOIN crm_tag t
                 ON t.id = pt.tag_id AND t.organization_id = pt.organization_id
               WHERE pt.organization_id = p.organization_id AND pt.prospect_id = p.id
                 AND t.active = TRUE AND lower(t.name) LIKE ? ESCAPE '\\'
             ))
          """);
      String query = "%" + escapedLike(filter.query().trim().toLowerCase(Locale.ROOT)) + "%";
      for (int index = 0; index < 9; index++) {
        parameters.add(query);
      }
    }
    return where.toString();
  }

  private String escapedLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private OperationalProspectView prospect(ResultSet resultSet, int rowNumber) throws SQLException {
    return new OperationalProspectView(
        resultSet.getObject("id", UUID.class),
        resultSet.getLong("version"),
        resultSet.getObject("institution_id", UUID.class),
        resultSet.getString("name"),
        resultSet.getString("legal_name"),
        ProspectStatus.valueOf(resultSet.getString("status")),
        ProspectEligibility.valueOf(resultSet.getString("eligibility")),
        nullableInteger(resultSet, "priority"),
        nullableInteger(resultSet, "score"),
        nullableInteger(resultSet, "estimated_students"),
        resultSet.getString("source"),
        resultSet.getString("source_detail"),
        resultSet.getObject("owner_user_id", UUID.class),
        resultSet.getString("owner_name"),
        resultSet.getString("website"),
        resultSet.getString("address"),
        resultSet.getString("locality"),
        resultSet.getString("province"),
        resultSet.getString("country"),
        resultSet.getString("timezone"),
        resultSet.getString("notes_summary"),
        instant(resultSet, "next_action_at"),
        instant(resultSet, "last_contact_at"),
        resultSet.getBoolean("contact_eligible"),
        resultSet.getString("lost_reason"),
        instant(resultSet, "status_detail_at"),
        instant(resultSet, "archived_at"),
        instant(resultSet, "created_at"),
        instant(resultSet, "updated_at"));
  }

  private void validate(UpdateProspectCommand command) {
    required(command.displayName(), "Display name");
    if (command.priority() != null && (command.priority() < 0 || command.priority() > 5)) {
      throw new IllegalArgumentException("Priority must be between 0 and 5");
    }
    if (command.score() != null && (command.score() < 0 || command.score() > 100)) {
      throw new IllegalArgumentException("Score must be between 0 and 100");
    }
    if (command.estimatedStudents() != null && command.estimatedStudents() < 0) {
      throw new IllegalArgumentException("Estimated students cannot be negative");
    }
  }

  private void validateTransition(OperationalProspectView before, TransitionCommand command) {
    if (command.status() == ProspectStatus.LOST
        && (command.reason() == null
            || !LOSS_REASONS.contains(command.reason().toUpperCase(Locale.ROOT)))) {
      throw new UnprocessableEntityException("LOST requires a valid loss reason");
    }
    if (command.status() == ProspectStatus.CONTACTED && !hasOutboundContact(before.id())) {
      throw new UnprocessableEntityException("CONTACTED requires a recorded outbound activity");
    }
    if (command.status() == ProspectStatus.DEMO_SCHEDULED && command.scheduledAt() == null) {
      throw new UnprocessableEntityException("DEMO_SCHEDULED requires a date");
    }
    if (command.status() == ProspectStatus.PROPOSAL
        && !hasOpportunity(before.id())
        && (!command.proposalException() || trim(command.comment()) == null)) {
      throw new UnprocessableEntityException(
          "PROPOSAL requires an opportunity or a documented exception");
    }
    if (before.status() == ProspectStatus.DO_NOT_CONTACT) {
      if (!currentActor.requiredPrincipal().permissions().contains("SETTINGS_MANAGE")) {
        throw new UnprocessableEntityException("Leaving DO_NOT_CONTACT requires SETTINGS_MANAGE");
      }
      if (hasActiveExclusion(before.id())) {
        throw new UnprocessableEntityException("Active exclusions must be removed first");
      }
    }
  }

  private void updateLifecycle(
      UUID id, long version, ProspectStatus status, String reason, Instant statusDetailAt) {
    ProspectEligibility eligibility =
        status == ProspectStatus.DO_NOT_CONTACT
            ? ProspectEligibility.EXCLUDED
            : status == ProspectStatus.CUSTOMER
                ? ProspectEligibility.CUSTOMER
                : ProspectEligibility.ELIGIBLE;
    boolean contactEligible = eligibility == ProspectEligibility.ELIGIBLE;
    int updated =
        jdbcTemplate.update(
            """
            UPDATE prospect SET status = ?, eligibility = ?, contact_eligible = ?, lost_reason = ?,
              status_detail_at = ?, archived_at = NULL, updated_by = ?, updated_at = now(),
              version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            status.name(),
            eligibility.name(),
            contactEligible,
            status == ProspectStatus.LOST ? trim(reason) : null,
            timestamp(statusDetailAt),
            currentActor.userIdOrNull(),
            id,
            currentActor.organizationId(),
            version);
    if (updated == 0) {
      requireExistsOrConflict(id, version);
    }
  }

  private void history(
      UUID prospectId,
      ProspectStatus previous,
      ProspectStatus next,
      String reason,
      String comment,
      String source) {
    jdbcTemplate.update(
        """
        INSERT INTO prospect_status_history (
          id, organization_id, prospect_id, actor_user_id, previous_status, new_status,
          reason, comment, source, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
        """,
        UUID.randomUUID(),
        currentActor.organizationId(),
        prospectId,
        currentActor.userIdOrNull(),
        previous.name(),
        next.name(),
        reason,
        comment,
        source);
  }

  private void requireExistsOrConflict(UUID id, long version) {
    Long actual =
        jdbcTemplate
            .query(
                "SELECT version FROM prospect WHERE id = ? AND organization_id = ?",
                (resultSet, rowNumber) -> resultSet.getLong(1),
                id,
                currentActor.organizationId())
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + id));
    throw new OptimisticConflictException(
        "Prospect was modified by another user (expected " + version + ", actual " + actual + ")");
  }

  private boolean validOwner(UUID userId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM app_user u JOIN organization_membership m ON m.user_id = u.id
            WHERE u.id = ? AND m.organization_id = ? AND u.active = TRUE AND m.active = TRUE
            """,
            Integer.class,
            userId,
            currentActor.organizationId());
    return count != null && count == 1;
  }

  private boolean hasOutboundContact(UUID prospectId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM activity
            WHERE organization_id = ? AND prospect_id = ? AND direction = 'OUTBOUND'
              AND activity_type IN (
                'EMAIL_SENT_MANUALLY', 'EMAIL_SENT_BY_SYSTEM', 'WHATSAPP_SENT_MANUALLY',
                'WHATSAPP_SENT_BY_SYSTEM', 'PHONE_CALL', 'MEETING', 'DEMO'
              )
            """,
            Integer.class,
            currentActor.organizationId(),
            prospectId);
    return count != null && count > 0;
  }

  private boolean hasOpportunity(UUID prospectId) {
    return false;
  }

  private boolean hasActiveExclusion(UUID prospectId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM prospect p
            JOIN contact c ON c.institution_id = p.institution_id AND c.organization_id = p.organization_id
            JOIN contact_channel cc ON cc.contact_id = c.id AND cc.organization_id = p.organization_id
            JOIN exclusion e ON e.organization_id = p.organization_id
              AND e.channel_type = cc.type AND e.normalized_value = cc.normalized_value
            WHERE p.id = ? AND p.organization_id = ? AND c.deleted_at IS NULL
            """,
            Integer.class,
            prospectId,
            currentActor.organizationId());
    return count != null && count > 0;
  }

  private Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
    int value = resultSet.getInt(column);
    return resultSet.wasNull() ? null : value;
  }

  private Instant instant(ResultSet resultSet, String column) throws SQLException {
    OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private java.sql.Timestamp timestamp(Instant value) {
    return value == null ? null : java.sql.Timestamp.from(value);
  }

  private String required(String value, String field) {
    String result = trim(value);
    if (result == null) {
      throw new IllegalArgumentException(field + " is required");
    }
    return result;
  }

  private String trim(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String value(Object value) {
    return value == null ? "" : value.toString();
  }

  private String csv(String value) {
    return CsvSafety.cell(value);
  }

  public record SearchFilter(
      String query,
      ProspectStatus status,
      UUID ownerUserId,
      boolean archived,
      int page,
      int size,
      String sort,
      String direction) {}

  public record UpdateProspectCommand(
      long version,
      String displayName,
      String legalName,
      Integer priority,
      Integer score,
      Integer estimatedStudents,
      String source,
      String sourceDetail,
      UUID ownerUserId,
      String website,
      String address,
      String city,
      String province,
      String country,
      String timezone,
      String notesSummary,
      Instant nextActionAt) {}

  public record TransitionCommand(
      long version,
      ProspectStatus status,
      String reason,
      String comment,
      Instant scheduledAt,
      boolean proposalException) {}

  public record OperationalProspectView(
      UUID id,
      long version,
      UUID institutionId,
      String displayName,
      String legalName,
      ProspectStatus status,
      ProspectEligibility eligibility,
      Integer priority,
      Integer score,
      Integer estimatedStudents,
      String source,
      String sourceDetail,
      UUID ownerUserId,
      String ownerName,
      String website,
      String address,
      String city,
      String province,
      String country,
      String timezone,
      String notesSummary,
      Instant nextActionAt,
      Instant lastContactAt,
      boolean contactEligible,
      String lostReason,
      Instant statusDetailAt,
      Instant archivedAt,
      Instant createdAt,
      Instant updatedAt) {}

  public record PageResult<T>(
      List<T> content, long totalElements, int totalPages, int number, int size) {}
}
