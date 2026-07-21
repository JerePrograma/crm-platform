package com.gestudio.crm.activity;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.common.UnprocessableEntityException;
import com.gestudio.crm.security.CurrentActor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class TimelineService {

  private static final Set<ActivityType> CONTACT_ACTIVITIES =
      Set.of(
          ActivityType.EMAIL_SENT_MANUALLY,
          ActivityType.EMAIL_SENT_BY_SYSTEM,
          ActivityType.EMAIL_RECEIVED,
          ActivityType.WHATSAPP_SENT_MANUALLY,
          ActivityType.WHATSAPP_SENT_BY_SYSTEM,
          ActivityType.WHATSAPP_RECEIVED,
          ActivityType.PHONE_CALL,
          ActivityType.MEETING,
          ActivityType.DEMO);

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final CurrentActor currentActor;
  private final AuditEventWriter auditEventWriter;

  public TimelineService(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      CurrentActor currentActor,
      AuditEventWriter auditEventWriter) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.currentActor = currentActor;
    this.auditEventWriter = auditEventWriter;
  }

  @Transactional
  public NoteView createNote(UUID prospectId, String body) {
    requireProspect(prospectId);
    UUID id = UUID.randomUUID();
    String sanitized = sanitize(body);
    jdbcTemplate.update(
        """
        INSERT INTO prospect_note (
          id, organization_id, prospect_id, author_user_id, body, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, now(), now())
        """,
        id,
        currentActor.organizationId(),
        prospectId,
        currentActor.userIdOrNull(),
        sanitized);
    auditEventWriter.record("NOTE_CREATED", "ProspectNote", id, Map.of("prospectId", prospectId));
    return note(id);
  }

  @Transactional
  public NoteView updateNote(UUID noteId, String body) {
    int updated =
        jdbcTemplate.update(
            """
            UPDATE prospect_note SET body = ?, updated_at = now()
            WHERE id = ? AND organization_id = ? AND deleted_at IS NULL
            """,
            sanitize(body),
            noteId,
            currentActor.organizationId());
    if (updated == 0) {
      throw new ResourceNotFoundException("Note not found: " + noteId);
    }
    auditEventWriter.record("NOTE_UPDATED", "ProspectNote", noteId, Map.of());
    return note(noteId);
  }

  @Transactional
  public void deleteNote(UUID noteId) {
    int updated =
        jdbcTemplate.update(
            "UPDATE prospect_note SET deleted_at = now(), updated_at = now() WHERE id = ? AND organization_id = ? AND deleted_at IS NULL",
            noteId,
            currentActor.organizationId());
    if (updated == 0) {
      throw new ResourceNotFoundException("Note not found: " + noteId);
    }
    auditEventWriter.record("NOTE_REMOVED", "ProspectNote", noteId, Map.of());
  }

  @Transactional
  public ActivityView createActivity(UUID prospectId, CreateActivityCommand command) {
    requireProspect(prospectId);
    if (command.contactId() != null && !contactBelongsToProspect(prospectId, command.contactId())) {
      throw new UnprocessableEntityException("Contact does not belong to the prospect");
    }
    UUID id = UUID.randomUUID();
    String summary = required(command.summary(), "Summary");
    String metadata = json(command.metadata() == null ? Map.of() : command.metadata());
    try {
      jdbcTemplate.update(
          """
          INSERT INTO activity (
            id, organization_id, prospect_id, contact_id, actor_user_id, activity_type,
            occurred_at, channel, direction, outcome, summary, detail, external_reference,
            metadata, created_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, now())
          """,
          id,
          currentActor.organizationId(),
          prospectId,
          command.contactId(),
          currentActor.userIdOrNull(),
          command.type().name(),
          timestamp(command.occurredAt() == null ? Instant.now() : command.occurredAt()),
          trim(command.channel()),
          command.direction().name(),
          trim(command.outcome()),
          summary,
          trim(command.detail()),
          trim(command.externalReference()),
          metadata);
    } catch (org.springframework.dao.DuplicateKeyException exception) {
      throw new com.gestudio.crm.common.DuplicateResourceException(
          "External activity reference already exists");
    }
    if (CONTACT_ACTIVITIES.contains(command.type())) {
      jdbcTemplate.update(
          """
          UPDATE prospect SET last_contact_at = GREATEST(coalesce(last_contact_at, ?), ?),
            updated_at = now(), updated_by = ?, version = version + 1
          WHERE id = ? AND organization_id = ?
          """,
          timestamp(command.occurredAt() == null ? Instant.now() : command.occurredAt()),
          timestamp(command.occurredAt() == null ? Instant.now() : command.occurredAt()),
          currentActor.userIdOrNull(),
          prospectId,
          currentActor.organizationId());
    }
    auditEventWriter.record(
        "ACTIVITY_CREATED", "Activity", id, Map.of("type", command.type().name()));
    return activity(id);
  }

  @Transactional
  public TaskView createTask(UUID prospectId, CreateTaskCommand command) {
    requireProspect(prospectId);
    UUID owner =
        command.ownerUserId() == null
            ? currentActor.requiredPrincipal().userId()
            : command.ownerUserId();
    requireOwner(owner);
    validateTaskDates(command.dueAt(), command.reminderAt());
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO crm_task (
          id, version, organization_id, prospect_id, owner_user_id, creator_user_id,
          title, description, due_at, priority, status, task_type, reminder_at,
          created_at, updated_at
        ) VALUES (?, 0, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, ?, now(), now())
        """,
        id,
        currentActor.organizationId(),
        prospectId,
        owner,
        currentActor.userIdOrNull(),
        required(command.title(), "Title"),
        trim(command.description()),
        timestamp(command.dueAt()),
        command.priority().name(),
        required(command.taskType(), "Task type"),
        timestamp(command.reminderAt()));
    refreshNextAction(prospectId);
    auditEventWriter.record("TASK_CREATED", "Task", id, Map.of("prospectId", prospectId));
    return task(id);
  }

  @Transactional
  public TaskView updateTask(UUID taskId, UpdateTaskCommand command) {
    TaskView before = task(taskId);
    requireOwner(command.ownerUserId());
    validateTaskDates(command.dueAt(), command.reminderAt());
    int updated =
        jdbcTemplate.update(
            """
            UPDATE crm_task SET owner_user_id = ?, title = ?, description = ?, due_at = ?,
              priority = ?, task_type = ?, reminder_at = ?, updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
              AND status IN ('OPEN', 'IN_PROGRESS')
            """,
            command.ownerUserId(),
            required(command.title(), "Title"),
            trim(command.description()),
            timestamp(command.dueAt()),
            command.priority().name(),
            required(command.taskType(), "Task type"),
            timestamp(command.reminderAt()),
            taskId,
            currentActor.organizationId(),
            command.version());
    if (updated == 0) {
      taskConflict(taskId, command.version());
    }
    refreshNextAction(before.prospectId());
    auditEventWriter.record("TASK_UPDATED", "Task", taskId, Map.of());
    return task(taskId);
  }

  @Transactional
  public TaskView changeTaskStatus(UUID taskId, long version, TaskStatus status, String outcome) {
    TaskView before = task(taskId);
    if (!allowedTaskTransition(before.status(), status)) {
      throw new UnprocessableEntityException(
          "Task transition from " + before.status() + " to " + status + " is not allowed");
    }
    int updated =
        jdbcTemplate.update(
            """
            UPDATE crm_task SET status = ?, outcome = ?,
              completed_at = CASE WHEN ? = 'COMPLETED' THEN now() ELSE NULL END,
              cancelled_at = CASE WHEN ? = 'CANCELLED' THEN now() ELSE NULL END,
              updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            status.name(),
            trim(outcome),
            status.name(),
            status.name(),
            taskId,
            currentActor.organizationId(),
            version);
    if (updated == 0) {
      taskConflict(taskId, version);
    }
    refreshNextAction(before.prospectId());
    auditEventWriter.record("TASK_STATUS_CHANGED", "Task", taskId, Map.of("status", status.name()));
    return task(taskId);
  }

  @Transactional(readOnly = true)
  public List<TaskView> listTasks(
      TaskStatus status, UUID ownerUserId, Instant dueBefore, int limit) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT * FROM crm_task WHERE organization_id = ? AND (?::text IS NULL OR status = ?::text) AND (?::uuid IS NULL OR owner_user_id = ?::uuid) AND (?::timestamptz IS NULL OR due_at <= ?::timestamptz) ORDER BY due_at, id LIMIT ?");
    return jdbcTemplate.query(
        sql.toString(),
        this::mapTask,
        currentActor.organizationId(),
        status == null ? null : status.name(),
        status == null ? null : status.name(),
        ownerUserId,
        ownerUserId,
        timestamp(dueBefore),
        timestamp(dueBefore),
        Math.min(Math.max(limit, 1), 500));
  }

  @Transactional(readOnly = true)
  public TimelinePage timeline(UUID prospectId, int page, int size) {
    requireProspect(prospectId);
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    String union =
        """
        SELECT id, created_at AS event_at, 'STATUS' AS event_type,
               previous_status || ' → ' || new_status AS title,
               coalesce(comment, reason) AS detail, actor_user_id, '{}'::jsonb AS metadata
        FROM prospect_status_history WHERE organization_id = ? AND prospect_id = ?
        UNION ALL
        SELECT id, created_at, 'NOTE', 'Nota', body, author_user_id, '{}'::jsonb
        FROM prospect_note WHERE organization_id = ? AND prospect_id = ? AND deleted_at IS NULL
        UNION ALL
        SELECT id, occurred_at, 'ACTIVITY', activity_type, summary, actor_user_id, metadata
        FROM activity WHERE organization_id = ? AND prospect_id = ?
        UNION ALL
        SELECT id, created_at, 'TASK', task_type || ': ' || title,
               status || ' · vence ' || due_at::text, creator_user_id, '{}'::jsonb
        FROM crm_task WHERE organization_id = ? AND prospect_id = ?
        UNION ALL
        SELECT id, created_at, 'AUDIT', action, payload::text, actor_user_id, '{}'::jsonb
        FROM audit_event WHERE organization_id = ? AND entity_type = 'Prospect' AND entity_id = ?
        """;
    Object[] keys = timelineKeys(prospectId);
    Long total =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM (" + union + ") events", Long.class, keys);
    Object[] paged = java.util.Arrays.copyOf(keys, keys.length + 2);
    paged[paged.length - 2] = safeSize;
    paged[paged.length - 1] = safePage * safeSize;
    List<TimelineItem> content =
        jdbcTemplate.query(
            "SELECT * FROM (" + union + ") events ORDER BY event_at DESC, id DESC LIMIT ? OFFSET ?",
            this::timelineItem,
            paged);
    return new TimelinePage(content, total == null ? 0 : total, safePage, safeSize);
  }

  private Object[] timelineKeys(UUID prospectId) {
    UUID organizationId = currentActor.organizationId();
    return new Object[] {
      organizationId,
      prospectId,
      organizationId,
      prospectId,
      organizationId,
      prospectId,
      organizationId,
      prospectId,
      organizationId,
      prospectId.toString()
    };
  }

  private void refreshNextAction(UUID prospectId) {
    jdbcTemplate.update(
        """
        UPDATE prospect SET next_action_at = (
          SELECT min(due_at) FROM crm_task
          WHERE organization_id = ? AND prospect_id = ? AND status IN ('OPEN', 'IN_PROGRESS')
        ), updated_at = now(), updated_by = ?, version = version + 1
        WHERE id = ? AND organization_id = ?
        """,
        currentActor.organizationId(),
        prospectId,
        currentActor.userIdOrNull(),
        prospectId,
        currentActor.organizationId());
  }

  private void requireProspect(UUID prospectId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM prospect WHERE id = ? AND organization_id = ?",
            Integer.class,
            prospectId,
            currentActor.organizationId());
    if (count == null || count == 0) {
      throw new ResourceNotFoundException("Prospect not found: " + prospectId);
    }
  }

  private void requireOwner(UUID owner) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM app_user u JOIN organization_membership m ON m.user_id = u.id
            WHERE u.id = ? AND m.organization_id = ? AND u.active = TRUE AND m.active = TRUE
            """,
            Integer.class,
            owner,
            currentActor.organizationId());
    if (count == null || count == 0) {
      throw new UnprocessableEntityException("Task owner must be an active organization member");
    }
  }

  private boolean contactBelongsToProspect(UUID prospectId, UUID contactId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM prospect p JOIN contact c ON c.institution_id = p.institution_id
              AND c.organization_id = p.organization_id
            WHERE p.id = ? AND c.id = ? AND p.organization_id = ? AND c.deleted_at IS NULL
            """,
            Integer.class,
            prospectId,
            contactId,
            currentActor.organizationId());
    return count != null && count == 1;
  }

  private void validateTaskDates(Instant dueAt, Instant reminderAt) {
    if (dueAt == null) {
      throw new IllegalArgumentException("Due date is required");
    }
    if (reminderAt != null && reminderAt.isAfter(dueAt)) {
      throw new IllegalArgumentException("Reminder cannot be after due date");
    }
  }

  private boolean allowedTaskTransition(TaskStatus from, TaskStatus to) {
    return switch (from) {
      case OPEN ->
          to == TaskStatus.IN_PROGRESS || to == TaskStatus.COMPLETED || to == TaskStatus.CANCELLED;
      case IN_PROGRESS ->
          to == TaskStatus.OPEN || to == TaskStatus.COMPLETED || to == TaskStatus.CANCELLED;
      case COMPLETED, CANCELLED -> to == TaskStatus.OPEN;
    };
  }

  private NoteView note(UUID id) {
    return jdbcTemplate
        .query(
            """
            SELECT id, prospect_id, author_user_id, body, created_at, updated_at
            FROM prospect_note WHERE id = ? AND organization_id = ? AND deleted_at IS NULL
            """,
            (resultSet, rowNumber) ->
                new NoteView(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("prospect_id", UUID.class),
                    resultSet.getObject("author_user_id", UUID.class),
                    resultSet.getString("body"),
                    instant(resultSet, "created_at"),
                    instant(resultSet, "updated_at")),
            id,
            currentActor.organizationId())
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + id));
  }

  private ActivityView activity(UUID id) {
    return jdbcTemplate
        .query(
            "SELECT * FROM activity WHERE id = ? AND organization_id = ?",
            this::mapActivity,
            id,
            currentActor.organizationId())
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + id));
  }

  private ActivityView mapActivity(ResultSet resultSet, int rowNumber) throws SQLException {
    return new ActivityView(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("prospect_id", UUID.class),
        resultSet.getObject("contact_id", UUID.class),
        resultSet.getObject("actor_user_id", UUID.class),
        ActivityType.valueOf(resultSet.getString("activity_type")),
        instant(resultSet, "occurred_at"),
        resultSet.getString("channel"),
        ActivityDirection.valueOf(resultSet.getString("direction")),
        resultSet.getString("outcome"),
        resultSet.getString("summary"),
        resultSet.getString("detail"),
        resultSet.getString("external_reference"),
        resultSet.getString("metadata"));
  }

  private TaskView task(UUID id) {
    return jdbcTemplate
        .query(
            "SELECT * FROM crm_task WHERE id = ? AND organization_id = ?",
            this::mapTask,
            id,
            currentActor.organizationId())
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
  }

  private TaskView mapTask(ResultSet resultSet, int rowNumber) throws SQLException {
    return new TaskView(
        resultSet.getObject("id", UUID.class),
        resultSet.getLong("version"),
        resultSet.getObject("prospect_id", UUID.class),
        resultSet.getObject("owner_user_id", UUID.class),
        resultSet.getObject("creator_user_id", UUID.class),
        resultSet.getString("title"),
        resultSet.getString("description"),
        instant(resultSet, "due_at"),
        TaskPriority.valueOf(resultSet.getString("priority")),
        TaskStatus.valueOf(resultSet.getString("status")),
        resultSet.getString("task_type"),
        instant(resultSet, "reminder_at"),
        instant(resultSet, "completed_at"),
        instant(resultSet, "cancelled_at"),
        resultSet.getString("outcome"),
        instant(resultSet, "created_at"),
        instant(resultSet, "updated_at"));
  }

  private void taskConflict(UUID id, long version) {
    Long actual =
        jdbcTemplate
            .query(
                "SELECT version FROM crm_task WHERE id = ? AND organization_id = ?",
                (resultSet, rowNumber) -> resultSet.getLong(1),
                id,
                currentActor.organizationId())
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    throw new OptimisticConflictException(
        "Task was modified by another user (expected " + version + ", actual " + actual + ")");
  }

  private TimelineItem timelineItem(ResultSet resultSet, int rowNumber) throws SQLException {
    return new TimelineItem(
        resultSet.getObject("id", UUID.class),
        instant(resultSet, "event_at"),
        resultSet.getString("event_type"),
        resultSet.getString("title"),
        resultSet.getString("detail"),
        resultSet.getObject("actor_user_id", UUID.class),
        resultSet.getString("metadata"));
  }

  private String sanitize(String body) {
    String value = required(body, "Note");
    if (value.length() > 10000) {
      throw new IllegalArgumentException("Note cannot exceed 10000 characters");
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private String json(Map<String, Object> metadata) {
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Activity metadata must be valid JSON", exception);
    }
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

  private java.sql.Timestamp timestamp(Instant value) {
    return value == null ? null : java.sql.Timestamp.from(value);
  }

  private Instant instant(ResultSet resultSet, String column) throws SQLException {
    OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  public record CreateActivityCommand(
      UUID contactId,
      ActivityType type,
      Instant occurredAt,
      String channel,
      ActivityDirection direction,
      String outcome,
      String summary,
      String detail,
      String externalReference,
      Map<String, Object> metadata) {}

  public record CreateTaskCommand(
      UUID ownerUserId,
      String title,
      String description,
      Instant dueAt,
      TaskPriority priority,
      String taskType,
      Instant reminderAt) {}

  public record UpdateTaskCommand(
      long version,
      UUID ownerUserId,
      String title,
      String description,
      Instant dueAt,
      TaskPriority priority,
      String taskType,
      Instant reminderAt) {}

  public record NoteView(
      UUID id,
      UUID prospectId,
      UUID authorUserId,
      String body,
      Instant createdAt,
      Instant updatedAt) {}

  public record ActivityView(
      UUID id,
      UUID prospectId,
      UUID contactId,
      UUID actorUserId,
      ActivityType type,
      Instant occurredAt,
      String channel,
      ActivityDirection direction,
      String outcome,
      String summary,
      String detail,
      String externalReference,
      String metadata) {}

  public record TaskView(
      UUID id,
      long version,
      UUID prospectId,
      UUID ownerUserId,
      UUID creatorUserId,
      String title,
      String description,
      Instant dueAt,
      TaskPriority priority,
      TaskStatus status,
      String taskType,
      Instant reminderAt,
      Instant completedAt,
      Instant cancelledAt,
      String outcome,
      Instant createdAt,
      Instant updatedAt) {}

  public record TimelineItem(
      UUID id,
      Instant eventAt,
      String eventType,
      String title,
      String detail,
      UUID actorUserId,
      String metadata) {}

  public record TimelinePage(
      List<TimelineItem> content, long totalElements, int number, int size) {}
}
