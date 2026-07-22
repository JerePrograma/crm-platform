package com.gestudio.crm.prospect;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.security.CurrentActor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final NormalizationService normalizationService;
  private final AuditEventWriter auditEventWriter;

  public TagService(
      JdbcTemplate jdbcTemplate,
      CurrentActor currentActor,
      NormalizationService normalizationService,
      AuditEventWriter auditEventWriter) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.normalizationService = normalizationService;
    this.auditEventWriter = auditEventWriter;
  }

  @Transactional(readOnly = true)
  public List<TagView> list(boolean includeInactive) {
    return jdbcTemplate.query(
        """
        SELECT t.id, t.version, t.name, t.color, t.active, t.created_at, t.updated_at,
          count(pt.prospect_id) AS usage_count
        FROM crm_tag t LEFT JOIN prospect_tag pt
          ON pt.organization_id = t.organization_id AND pt.tag_id = t.id
        WHERE t.organization_id = ? AND (? OR t.active = TRUE)
        GROUP BY t.id ORDER BY t.active DESC, t.normalized_name, t.id
        """,
        this::view,
        currentActor.organizationId(),
        includeInactive);
  }

  @Transactional
  public TagView create(String name, String color) {
    validate(name, color);
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO crm_tag (
          id, version, organization_id, name, normalized_name, color, active,
          created_by, created_at, updated_at
        ) VALUES (?, 0, ?, ?, ?, ?, TRUE, ?, now(), now())
        """,
        id,
        currentActor.organizationId(),
        name.trim(),
        normalizationService.normalizeName(name),
        color,
        currentActor.userIdOrNull());
    auditEventWriter.record("TAG_CREATED", "TAG", id, Map.of("name", name.trim()));
    return get(id);
  }

  @Transactional
  public TagView update(UUID id, long version, String name, String color, boolean active) {
    validate(name, color);
    int updated =
        jdbcTemplate.update(
            """
            UPDATE crm_tag SET name = ?, normalized_name = ?, color = ?, active = ?,
              version = version + 1, updated_at = now()
            WHERE organization_id = ? AND id = ? AND version = ?
            """,
            name.trim(),
            normalizationService.normalizeName(name),
            color,
            active,
            currentActor.organizationId(),
            id,
            version);
    if (updated != 1) {
      requireExistsOrConflict(id, version);
    }
    auditEventWriter.record(
        "TAG_UPDATED", "TAG", id, Map.of("name", name.trim(), "active", active));
    return get(id);
  }

  @Transactional
  public TagView deactivate(UUID id, long version) {
    TagView current = get(id);
    return update(id, version, current.name(), current.color(), false);
  }

  @Transactional
  public void assign(UUID tagId, List<UUID> prospectIds) {
    if (prospectIds == null || prospectIds.isEmpty() || prospectIds.size() > 200) {
      throw new IllegalArgumentException(
          "Bulk tag assignment requires between 1 and 200 prospects");
    }
    TagView tag = get(tagId);
    if (!tag.active()) {
      throw new IllegalArgumentException("Inactive tags cannot be assigned");
    }
    List<UUID> distinctProspectIds = prospectIds.stream().distinct().toList();
    for (UUID prospectId : distinctProspectIds) {
      Integer existing =
          jdbcTemplate.queryForObject(
              "SELECT count(*) FROM prospect WHERE organization_id = ? AND id = ? AND archived_at IS NULL",
              Integer.class,
              currentActor.organizationId(),
              prospectId);
      if (existing == null || existing != 1) {
        throw new ResourceNotFoundException(
            "One or more prospects were not found in the organization");
      }
    }
    for (UUID prospectId : distinctProspectIds) {
      jdbcTemplate.update(
          """
          INSERT INTO prospect_tag (organization_id, prospect_id, tag_id, assigned_by, assigned_at)
          VALUES (?, ?, ?, ?, now()) ON CONFLICT DO NOTHING
          """,
          currentActor.organizationId(),
          prospectId,
          tagId,
          currentActor.userIdOrNull());
    }
    auditEventWriter.record(
        "TAG_ASSIGNED_BULK", "TAG", tagId, Map.of("prospectCount", distinctProspectIds.size()));
  }

  @Transactional
  public void unassign(UUID tagId, UUID prospectId) {
    int updated =
        jdbcTemplate.update(
            """
            DELETE FROM prospect_tag WHERE organization_id = ? AND tag_id = ? AND prospect_id = ?
            """,
            currentActor.organizationId(),
            tagId,
            prospectId);
    if (updated == 0) {
      throw new ResourceNotFoundException("Prospect tag assignment was not found");
    }
    auditEventWriter.record(
        "TAG_UNASSIGNED", "TAG", tagId, Map.of("prospectId", prospectId.toString()));
  }

  @Transactional(readOnly = true)
  public List<TagView> forProspect(UUID prospectId) {
    Integer prospect =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM prospect WHERE organization_id = ? AND id = ?",
            Integer.class,
            currentActor.organizationId(),
            prospectId);
    if (prospect == null || prospect != 1) {
      throw new ResourceNotFoundException("Prospect was not found");
    }
    return jdbcTemplate.query(
        """
        SELECT t.id, t.version, t.name, t.color, t.active, t.created_at, t.updated_at,
          (SELECT count(*) FROM prospect_tag used
           WHERE used.organization_id = t.organization_id AND used.tag_id = t.id) AS usage_count
        FROM prospect_tag pt JOIN crm_tag t
          ON t.organization_id = pt.organization_id AND t.id = pt.tag_id
        WHERE pt.organization_id = ? AND pt.prospect_id = ?
        ORDER BY t.normalized_name
        """,
        this::view,
        currentActor.organizationId(),
        prospectId);
  }

  private TagView get(UUID id) {
    return jdbcTemplate
        .query(
            """
            SELECT t.id, t.version, t.name, t.color, t.active, t.created_at, t.updated_at,
              count(pt.prospect_id) AS usage_count
            FROM crm_tag t LEFT JOIN prospect_tag pt
              ON pt.organization_id = t.organization_id AND pt.tag_id = t.id
            WHERE t.organization_id = ? AND t.id = ? GROUP BY t.id
            """,
            this::view,
            currentActor.organizationId(),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Tag was not found: " + id));
  }

  private TagView view(ResultSet rs, int row) throws SQLException {
    return new TagView(
        rs.getObject("id", UUID.class),
        rs.getLong("version"),
        rs.getString("name"),
        rs.getString("color"),
        rs.getBoolean("active"),
        rs.getLong("usage_count"),
        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
        rs.getObject("updated_at", OffsetDateTime.class).toInstant());
  }

  private void validate(String name, String color) {
    if (name == null || name.isBlank() || name.trim().length() > 80) {
      throw new IllegalArgumentException("Tag name is required and limited to 80 characters");
    }
    if (color == null || !color.matches("#[0-9A-Fa-f]{6}")) {
      throw new IllegalArgumentException("Tag color must be a six-digit hex color");
    }
  }

  private void requireExistsOrConflict(UUID id, long version) {
    TagView current = get(id);
    throw new OptimisticConflictException(
        "Tag changed concurrently (expected " + version + ", actual " + current.version() + ")");
  }

  public record TagView(
      UUID id,
      long version,
      String name,
      String color,
      boolean active,
      long usageCount,
      java.time.Instant createdAt,
      java.time.Instant updatedAt) {}
}
