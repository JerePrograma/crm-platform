package com.gestudio.crm.deduplication;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.common.UnprocessableEntityException;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.security.CurrentActor;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DuplicateResolutionService {

  private final JdbcTemplate jdbcTemplate;
  private final ProspectApplicationService prospectApplicationService;
  private final EntityManager entityManager;
  private final CurrentActor currentActor;
  private final AuditEventWriter auditEventWriter;

  public DuplicateResolutionService(
      JdbcTemplate jdbcTemplate,
      ProspectApplicationService prospectApplicationService,
      EntityManager entityManager,
      CurrentActor currentActor,
      AuditEventWriter auditEventWriter) {
    this.jdbcTemplate = jdbcTemplate;
    this.prospectApplicationService = prospectApplicationService;
    this.entityManager = entityManager;
    this.currentActor = currentActor;
    this.auditEventWriter = auditEventWriter;
  }

  @Transactional(readOnly = true)
  public List<ReviewView> queue() {
    return jdbcTemplate.query(
        reviewSelect()
            + " WHERE dr.organization_id = ? AND dr.status IN ('PENDING', 'DEFERRED') ORDER BY dr.created_at, dr.id",
        this::reviewView,
        currentActor.organizationId());
  }

  @Transactional(readOnly = true)
  public ReviewView get(UUID reviewId) {
    return review(reviewId, false).view();
  }

  @Transactional
  public ResolutionResult resolve(UUID reviewId, ResolutionCommand command) {
    validate(command);
    ReviewRow review = review(reviewId, true);
    if (!List.of("PENDING", "DEFERRED").contains(review.status())) {
      if (command.idempotencyKey().equals(review.resolutionKey())
          && command.action().name().equals(review.resolutionAction())) {
        return result(reviewId);
      }
      throw new OptimisticConflictException("Duplicate review was already resolved");
    }

    if (command.action() == DuplicateResolutionAction.DEFER) {
      jdbcTemplate.update(
          """
          UPDATE duplicate_review SET status = 'DEFERRED', resolution_action = 'DEFER',
            resolution_key = ?, resolution_comment = ?, resolved_by = ?, updated_at = now(),
            version = version + 1
          WHERE id = ? AND organization_id = ?
          """,
          command.idempotencyKey(),
          trim(command.comment()),
          currentActor.userIdOrNull(),
          reviewId,
          currentActor.organizationId());
      audit("DUPLICATE_REVIEW_DEFERRED", reviewId, command, null, null);
      return result(reviewId);
    }

    UUID survivor = null;
    UUID absorbed = null;
    switch (command.action()) {
      case MARK_NOT_DUPLICATE, CREATE_SEPARATE ->
          survivor = createSeparate(review, required(command.separateName(), "Separate name"));
      case LINK_TO_EXISTING -> {
        survivor = target(review, command.survivorProspectId());
        requireProspect(survivor);
        jdbcTemplate.update(
            "UPDATE import_row SET prospect_id = ?, status = 'DUPLICATE', error_message = NULL, updated_at = now(), version = version + 1 WHERE id = ? AND organization_id = ?",
            survivor,
            review.importRowId(),
            currentActor.organizationId());
      }
      case MERGE -> {
        survivor = target(review, command.survivorProspectId());
        absorbed =
            command.absorbedProspectId() == null
                ? review.rowProspectId()
                : command.absorbedProspectId();
        if (absorbed == null) {
          throw new UnprocessableEntityException(
              "MERGE requires an absorbed prospect because the import row has no prospect");
        }
        merge(reviewId, survivor, absorbed, command.idempotencyKey());
        jdbcTemplate.update(
            "UPDATE import_row SET prospect_id = ?, status = 'DUPLICATE', error_message = NULL, updated_at = now(), version = version + 1 WHERE id = ? AND organization_id = ?",
            survivor,
            review.importRowId(),
            currentActor.organizationId());
      }
      case REJECT_ROW ->
          jdbcTemplate.update(
              "UPDATE import_row SET prospect_id = NULL, status = 'REJECTED', error_message = ?, updated_at = now(), version = version + 1 WHERE id = ? AND organization_id = ?",
              trim(command.comment()) == null
                  ? "Rejected during duplicate review"
                  : trim(command.comment()),
              review.importRowId(),
              currentActor.organizationId());
      case DEFER -> throw new IllegalStateException("DEFER was handled before resolution");
    }

    jdbcTemplate.update(
        """
        UPDATE duplicate_review SET status = 'RESOLVED', resolution_action = ?, resolved_at = now(),
          resolved_by = ?, survivor_prospect_id = ?, absorbed_prospect_id = ?, resolution_key = ?,
          resolution_comment = ?, updated_at = now(), version = version + 1
        WHERE id = ? AND organization_id = ?
        """,
        command.action().name(),
        currentActor.userIdOrNull(),
        survivor,
        absorbed,
        command.idempotencyKey(),
        trim(command.comment()),
        reviewId,
        currentActor.organizationId());
    audit("DUPLICATE_REVIEW_RESOLVED", reviewId, command, survivor, absorbed);
    return result(reviewId);
  }

  private UUID createSeparate(ReviewRow review, String name) {
    String sourceId = "duplicate-review:" + review.id();
    UUID prospectId =
        prospectApplicationService
            .create(
                new CreateProspectCommand(
                    name,
                    null,
                    null,
                    null,
                    "Argentina",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    sourceId,
                    "DUPLICATE_REVIEW",
                    "Created from sanitized duplicate review evidence",
                    null,
                    null,
                    null,
                    null,
                    null,
                    Instant.now(),
                    null))
            .id();
    entityManager.flush();
    jdbcTemplate.update(
        "UPDATE import_row SET prospect_id = ?, status = 'ACCEPTED', error_message = NULL, updated_at = now(), version = version + 1 WHERE id = ? AND organization_id = ?",
        prospectId,
        review.importRowId(),
        currentActor.organizationId());
    return prospectId;
  }

  private void merge(UUID reviewId, UUID survivorId, UUID absorbedId, String idempotencyKey) {
    if (survivorId.equals(absorbedId)) {
      throw new UnprocessableEntityException("Survivor and absorbed prospects must be different");
    }
    List<ProspectLock> prospects =
        jdbcTemplate.query(
            """
            SELECT p.id, p.institution_id, p.merged_into_id
            FROM prospect p
            WHERE p.organization_id = ? AND p.id IN (?, ?)
            ORDER BY p.id FOR UPDATE
            """,
            (resultSet, rowNumber) ->
                new ProspectLock(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("institution_id", UUID.class),
                    resultSet.getObject("merged_into_id", UUID.class)),
            currentActor.organizationId(),
            survivorId,
            absorbedId);
    if (prospects.size() != 2) {
      throw new ResourceNotFoundException("Both prospects must exist in the current organization");
    }
    if (prospects.stream().anyMatch(prospect -> prospect.mergedIntoId() != null)) {
      throw new OptimisticConflictException("A selected prospect was already merged");
    }
    UUID survivorInstitution = institution(prospects, survivorId);
    UUID absorbedInstitution = institution(prospects, absorbedId);
    UUID mergeId = UUID.randomUUID();

    jdbcTemplate.update(
        """
        INSERT INTO prospect_merge_map (
          id, organization_id, survivor_prospect_id, absorbed_prospect_id,
          duplicate_review_id, merged_by, idempotency_key, merged_at, metadata
        ) VALUES (?, ?, ?, ?, ?, ?, ?, now(), '{"source":"duplicate-review"}'::jsonb)
        """,
        mergeId,
        currentActor.organizationId(),
        survivorId,
        absorbedId,
        reviewId,
        currentActor.userIdOrNull(),
        idempotencyKey);

    mergeContacts(mergeId, survivorInstitution, absorbedInstitution);
    moveReferences(survivorId, absorbedId);
    mergeInstitution(survivorInstitution, absorbedInstitution);
    mergeProspectFields(survivorId, absorbedId);

    jdbcTemplate.update(
        """
        UPDATE prospect SET status = 'DUPLICATE', eligibility = 'EXCLUDED', contact_eligible = FALSE,
          merged_into_id = ?, merged_at = now(), merged_by = ?, archived_at = now(),
          updated_by = ?, updated_at = now(), version = version + 1
        WHERE id = ? AND organization_id = ?
        """,
        survivorId,
        currentActor.userIdOrNull(),
        currentActor.userIdOrNull(),
        absorbedId,
        currentActor.organizationId());
  }

  private void mergeContacts(UUID mergeId, UUID survivorInstitution, UUID absorbedInstitution) {
    List<UUID> absorbedContacts =
        jdbcTemplate.query(
            "SELECT id FROM contact WHERE organization_id = ? AND institution_id = ? AND deleted_at IS NULL ORDER BY id FOR UPDATE",
            (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
            currentActor.organizationId(),
            absorbedInstitution);
    for (UUID absorbedContact : absorbedContacts) {
      UUID survivorContact = matchingContact(survivorInstitution, absorbedContact);
      if (survivorContact == null) {
        jdbcTemplate.update(
            "UPDATE contact SET institution_id = ?, updated_at = now(), version = version + 1 WHERE id = ? AND organization_id = ?",
            survivorInstitution,
            absorbedContact,
            currentActor.organizationId());
        continue;
      }
      jdbcTemplate.update(
          "UPDATE activity SET contact_id = ? WHERE contact_id = ? AND organization_id = ?",
          survivorContact,
          absorbedContact,
          currentActor.organizationId());
      jdbcTemplate.update(
          """
          DELETE FROM contact_channel absorbed
          USING contact_channel survivor
          WHERE absorbed.contact_id = ? AND survivor.contact_id = ?
            AND absorbed.organization_id = ? AND survivor.organization_id = absorbed.organization_id
            AND survivor.type = absorbed.type AND survivor.normalized_value = absorbed.normalized_value
          """,
          absorbedContact,
          survivorContact,
          currentActor.organizationId());
      jdbcTemplate.update(
          "UPDATE contact_channel SET contact_id = ?, updated_at = now(), version = version + 1 WHERE contact_id = ? AND organization_id = ?",
          survivorContact,
          absorbedContact,
          currentActor.organizationId());
      jdbcTemplate.update(
          "UPDATE contact SET deleted_at = now(), primary_contact = FALSE, updated_at = now(), version = version + 1 WHERE id = ? AND organization_id = ?",
          absorbedContact,
          currentActor.organizationId());
      jdbcTemplate.update(
          "INSERT INTO contact_merge_map (id, organization_id, survivor_contact_id, absorbed_contact_id, prospect_merge_id, merged_at) VALUES (?, ?, ?, ?, ?, now())",
          UUID.randomUUID(),
          currentActor.organizationId(),
          survivorContact,
          absorbedContact,
          mergeId);
    }
  }

  private UUID matchingContact(UUID survivorInstitution, UUID absorbedContact) {
    return jdbcTemplate
        .query(
            """
            SELECT survivor.id
            FROM contact survivor JOIN contact absorbed ON absorbed.id = ?
            WHERE survivor.organization_id = ? AND survivor.institution_id = ?
              AND survivor.deleted_at IS NULL
              AND (
                (lower(coalesce(survivor.name, '')) = lower(coalesce(absorbed.name, ''))
                  AND lower(coalesce(survivor.role, '')) = lower(coalesce(absorbed.role, '')))
                OR EXISTS (
                  SELECT 1 FROM contact_channel sc JOIN contact_channel ac
                    ON ac.type = sc.type AND ac.normalized_value = sc.normalized_value
                  WHERE sc.contact_id = survivor.id AND ac.contact_id = absorbed.id
                )
              )
            ORDER BY survivor.primary_contact DESC, survivor.id LIMIT 1
            """,
            (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
            absorbedContact,
            currentActor.organizationId(),
            survivorInstitution)
        .stream()
        .findFirst()
        .orElse(null);
  }

  private void moveReferences(UUID survivorId, UUID absorbedId) {
    jdbcTemplate.update(
        """
        UPDATE opportunity SET primary_active = FALSE
        WHERE organization_id = ? AND prospect_id = ? AND primary_active
          AND EXISTS (
            SELECT 1 FROM opportunity survivor
            WHERE survivor.organization_id = opportunity.organization_id
              AND survivor.prospect_id = ? AND survivor.primary_active
          )
        """,
        currentActor.organizationId(),
        absorbedId,
        survivorId);
    for (String table :
        List.of(
            "prospect_note", "activity", "crm_task", "prospect_status_history", "opportunity")) {
      jdbcTemplate.update(
          "UPDATE " + table + " SET prospect_id = ? WHERE prospect_id = ? AND organization_id = ?",
          survivorId,
          absorbedId,
          currentActor.organizationId());
    }
    jdbcTemplate.update(
        """
        DELETE FROM campaign_audience_recipient absorbed
        WHERE absorbed.organization_id = ? AND absorbed.prospect_id = ?
          AND EXISTS (
            SELECT 1 FROM campaign_audience_recipient survivor
            WHERE survivor.organization_id = absorbed.organization_id
              AND survivor.campaign_id = absorbed.campaign_id AND survivor.prospect_id = ?
          )
        """,
        currentActor.organizationId(),
        absorbedId,
        survivorId);
    jdbcTemplate.update(
        "UPDATE campaign_audience_recipient SET prospect_id = ? WHERE prospect_id = ? AND organization_id = ?",
        survivorId,
        absorbedId,
        currentActor.organizationId());
    jdbcTemplate.update(
        """
        DELETE FROM campaign_simulation_result absorbed
        WHERE absorbed.organization_id = ? AND absorbed.prospect_id = ?
          AND EXISTS (
            SELECT 1 FROM campaign_simulation_result survivor
            WHERE survivor.organization_id = absorbed.organization_id
              AND survivor.simulation_run_id = absorbed.simulation_run_id
              AND survivor.prospect_id = ?
          )
        """,
        currentActor.organizationId(),
        absorbedId,
        survivorId);
    jdbcTemplate.update(
        "UPDATE campaign_simulation_result SET prospect_id = ? WHERE prospect_id = ? AND organization_id = ?",
        survivorId,
        absorbedId,
        currentActor.organizationId());
    jdbcTemplate.update(
        "UPDATE import_row SET prospect_id = ? WHERE prospect_id = ? AND organization_id = ?",
        survivorId,
        absorbedId,
        currentActor.organizationId());
    jdbcTemplate.update(
        "UPDATE duplicate_review SET existing_prospect_id = ? WHERE existing_prospect_id = ? AND organization_id = ?",
        survivorId,
        absorbedId,
        currentActor.organizationId());
  }

  private void mergeInstitution(UUID survivorInstitution, UUID absorbedInstitution) {
    jdbcTemplate.update(
        """
        UPDATE institution survivor SET
          legal_name = coalesce(survivor.legal_name, absorbed.legal_name),
          category = coalesce(survivor.category, absorbed.category),
          website = coalesce(survivor.website, absorbed.website),
          website_domain = coalesce(survivor.website_domain, absorbed.website_domain),
          address = coalesce(survivor.address, absorbed.address),
          locality = coalesce(survivor.locality, absorbed.locality),
          normalized_locality = coalesce(survivor.normalized_locality, absorbed.normalized_locality),
          province = coalesce(survivor.province, absorbed.province),
          country = coalesce(survivor.country, absorbed.country),
          timezone = coalesce(survivor.timezone, absorbed.timezone),
          updated_at = now(), version = survivor.version + 1
        FROM institution absorbed
        WHERE survivor.id = ? AND absorbed.id = ?
          AND survivor.organization_id = ? AND absorbed.organization_id = survivor.organization_id
        """,
        survivorInstitution,
        absorbedInstitution,
        currentActor.organizationId());
  }

  private void mergeProspectFields(UUID survivorId, UUID absorbedId) {
    jdbcTemplate.update(
        """
        UPDATE prospect survivor SET
          priority = CASE WHEN survivor.priority IS NULL THEN absorbed.priority
            WHEN absorbed.priority IS NULL THEN survivor.priority ELSE greatest(survivor.priority, absorbed.priority) END,
          score = CASE WHEN survivor.score IS NULL THEN absorbed.score
            WHEN absorbed.score IS NULL THEN survivor.score ELSE greatest(survivor.score, absorbed.score) END,
          estimated_students = CASE WHEN survivor.estimated_students IS NULL THEN absorbed.estimated_students
            WHEN absorbed.estimated_students IS NULL THEN survivor.estimated_students
            ELSE greatest(survivor.estimated_students, absorbed.estimated_students) END,
          source_detail = coalesce(survivor.source_detail, absorbed.source_detail),
          owner_user_id = coalesce(survivor.owner_user_id, absorbed.owner_user_id),
          notes_summary = concat_ws(E'\n', nullif(survivor.notes_summary, ''), nullif(absorbed.notes_summary, '')),
          next_action_at = CASE WHEN survivor.next_action_at IS NULL THEN absorbed.next_action_at
            WHEN absorbed.next_action_at IS NULL THEN survivor.next_action_at
            ELSE least(survivor.next_action_at, absorbed.next_action_at) END,
          last_contact_at = CASE WHEN survivor.last_contact_at IS NULL THEN absorbed.last_contact_at
            WHEN absorbed.last_contact_at IS NULL THEN survivor.last_contact_at
            ELSE greatest(survivor.last_contact_at, absorbed.last_contact_at) END,
          updated_by = ?, updated_at = now(), version = survivor.version + 1
        FROM prospect absorbed
        WHERE survivor.id = ? AND absorbed.id = ?
          AND survivor.organization_id = ? AND absorbed.organization_id = survivor.organization_id
        """,
        currentActor.userIdOrNull(),
        survivorId,
        absorbedId,
        currentActor.organizationId());
  }

  private void requireProspect(UUID prospectId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM prospect WHERE id = ? AND organization_id = ? AND merged_into_id IS NULL",
            Integer.class,
            prospectId,
            currentActor.organizationId());
    if (count == null || count != 1) {
      throw new ResourceNotFoundException("Prospect not found in the current organization");
    }
  }

  private ReviewRow review(UUID id, boolean lock) {
    return jdbcTemplate
        .query(
            reviewSelect()
                + " WHERE dr.id = ? AND dr.organization_id = ?"
                + (lock ? " FOR UPDATE OF dr" : ""),
            this::reviewRow,
            id,
            currentActor.organizationId())
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Duplicate review not found: " + id));
  }

  private String reviewSelect() {
    return """
        SELECT dr.id, dr.version, dr.status, dr.match_type, dr.confidence, dr.notes,
          dr.existing_prospect_id, dr.resolution_action, dr.resolution_key, dr.created_at,
          ir.id AS import_row_id, ir.source_sheet, ir.row_number, ir.raw_data,
          ir.normalized_email, ir.normalized_phone, ir.prospect_id AS row_prospect_id,
          p.id AS candidate_id, i.name AS candidate_name, i.locality AS candidate_locality,
          i.website AS candidate_website, p.status AS candidate_status
        FROM duplicate_review dr
        JOIN import_row ir ON ir.id = dr.import_row_id AND ir.organization_id = dr.organization_id
        LEFT JOIN prospect p ON p.id = dr.existing_prospect_id AND p.organization_id = dr.organization_id
        LEFT JOIN institution i ON i.id = p.institution_id AND i.organization_id = p.organization_id
        """;
  }

  private ReviewRow reviewRow(ResultSet resultSet, int rowNumber) throws SQLException {
    ReviewView view = reviewView(resultSet, rowNumber);
    return new ReviewRow(
        view.id(),
        view.status(),
        view.existingProspectId(),
        resultSet.getObject("import_row_id", UUID.class),
        resultSet.getObject("row_prospect_id", UUID.class),
        resultSet.getString("resolution_action"),
        resultSet.getString("resolution_key"),
        view);
  }

  private ReviewView reviewView(ResultSet resultSet, int rowNumber) throws SQLException {
    UUID existingId = resultSet.getObject("candidate_id", UUID.class);
    ProspectCandidate existing =
        existingId == null
            ? null
            : new ProspectCandidate(
                existingId,
                resultSet.getString("candidate_name"),
                resultSet.getString("candidate_locality"),
                resultSet.getString("candidate_website"),
                resultSet.getString("candidate_status"));
    return new ReviewView(
        resultSet.getObject("id", UUID.class),
        resultSet.getLong("version"),
        resultSet.getString("status"),
        resultSet.getString("match_type"),
        resultSet.getBigDecimal("confidence"),
        resultSet.getString("notes"),
        resultSet.getObject("existing_prospect_id", UUID.class),
        resultSet.getString("source_sheet"),
        resultSet.getInt("row_number"),
        resultSet.getString("raw_data"),
        resultSet.getString("normalized_email"),
        resultSet.getString("normalized_phone"),
        existing,
        resultSet.getTimestamp("created_at").toInstant());
  }

  private ResolutionResult result(UUID reviewId) {
    return jdbcTemplate
        .query(
            """
            SELECT id, status, resolution_action, survivor_prospect_id, absorbed_prospect_id,
              resolved_at, resolution_comment
            FROM duplicate_review WHERE id = ? AND organization_id = ?
            """,
            (resultSet, rowNumber) ->
                new ResolutionResult(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("status"),
                    resultSet.getString("resolution_action"),
                    resultSet.getObject("survivor_prospect_id", UUID.class),
                    resultSet.getObject("absorbed_prospect_id", UUID.class),
                    resultSet.getTimestamp("resolved_at") == null
                        ? null
                        : resultSet.getTimestamp("resolved_at").toInstant(),
                    resultSet.getString("resolution_comment")),
            reviewId,
            currentActor.organizationId())
        .stream()
        .findFirst()
        .orElseThrow();
  }

  private UUID target(ReviewRow review, UUID requested) {
    UUID target = requested == null ? review.existingProspectId() : requested;
    if (target == null) {
      throw new UnprocessableEntityException("A survivor prospect is required");
    }
    return target;
  }

  private UUID institution(List<ProspectLock> prospects, UUID prospectId) {
    return prospects.stream()
        .filter(prospect -> prospect.id().equals(prospectId))
        .findFirst()
        .orElseThrow()
        .institutionId();
  }

  private void validate(ResolutionCommand command) {
    if (command == null || command.action() == null) {
      throw new IllegalArgumentException("Resolution action is required");
    }
    required(command.idempotencyKey(), "Idempotency key");
    if (command.idempotencyKey().length() > 200) {
      throw new IllegalArgumentException("Idempotency key is too long");
    }
  }

  private void audit(
      String action, UUID reviewId, ResolutionCommand command, UUID survivor, UUID absorbed) {
    java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
    payload.put("action", command.action().name());
    payload.put("survivorProspectId", survivor);
    payload.put("absorbedProspectId", absorbed);
    auditEventWriter.record(action, "DuplicateReview", reviewId, payload);
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

  private record ProspectLock(UUID id, UUID institutionId, UUID mergedIntoId) {}

  private record ReviewRow(
      UUID id,
      String status,
      UUID existingProspectId,
      UUID importRowId,
      UUID rowProspectId,
      String resolutionAction,
      String resolutionKey,
      ReviewView view) {}

  public record ResolutionCommand(
      DuplicateResolutionAction action,
      UUID survivorProspectId,
      UUID absorbedProspectId,
      String separateName,
      String comment,
      String idempotencyKey) {}

  public record ProspectCandidate(
      UUID id, String displayName, String locality, String website, String status) {}

  public record ReviewView(
      UUID id,
      long version,
      String status,
      String matchType,
      BigDecimal confidence,
      String matchReasons,
      UUID existingProspectId,
      String sourceSheet,
      int rowNumber,
      String sourceData,
      String normalizedEmail,
      String normalizedPhone,
      ProspectCandidate existingProspect,
      Instant createdAt) {}

  public record ResolutionResult(
      UUID reviewId,
      String status,
      String action,
      UUID survivorProspectId,
      UUID absorbedProspectId,
      Instant resolvedAt,
      String comment) {}
}
