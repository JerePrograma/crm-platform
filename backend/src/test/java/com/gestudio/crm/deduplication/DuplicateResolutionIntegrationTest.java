package com.gestudio.crm.deduplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.activity.ActivityDirection;
import com.gestudio.crm.activity.ActivityType;
import com.gestudio.crm.activity.TaskPriority;
import com.gestudio.crm.activity.TimelineService;
import com.gestudio.crm.activity.TimelineService.CreateActivityCommand;
import com.gestudio.crm.activity.TimelineService.CreateTaskCommand;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.deduplication.DuplicateResolutionService.ResolutionCommand;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.identity.IdentityService;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "security.bootstrap.username=duplicate-admin",
      "security.bootstrap.password=duplicate-password-1"
    })
@Testcontainers
class DuplicateResolutionIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private IdentityService identityService;
  @Autowired private ProspectApplicationService prospectApplicationService;
  @Autowired private DuplicateResolutionService duplicateResolutionService;
  @Autowired private TimelineService timelineService;
  @Autowired private JdbcTemplate jdbcTemplate;

  private CrmPrincipal principal;

  @BeforeEach
  void authenticate() {
    principal = (CrmPrincipal) identityService.loadUserByUsername("duplicate-admin");
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, principal.password(), principal.getAuthorities()));
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void mergesTwoProspectsTransactionallyAndIsIdempotent() {
    String suffix = UUID.randomUUID().toString();
    UUID survivor =
        createProspect("Academia Central " + suffix, "central." + suffix + "@test.invalid");
    UUID absorbed =
        createProspect("Academia Central Sede " + suffix, "sede." + suffix + "@test.invalid");

    timelineService.createNote(absorbed, "Nota preservada del prospecto absorbido");
    timelineService.createActivity(
        absorbed,
        new CreateActivityCommand(
            null,
            ActivityType.PHONE_CALL,
            Instant.parse("2026-07-21T18:00:00Z"),
            "PHONE",
            ActivityDirection.OUTBOUND,
            "CONNECTED",
            "Actividad preservada",
            null,
            "merge-activity-" + suffix,
            Map.of("synthetic", true)));
    timelineService.createTask(
        absorbed,
        new CreateTaskCommand(
            principal.userId(),
            "Tarea preservada",
            null,
            Instant.parse("2026-08-15T12:00:00Z"),
            TaskPriority.MEDIUM,
            "FOLLOW_UP",
            null));

    UUID reviewId = createReview(survivor, absorbed, 1);
    String key = "merge-" + suffix;
    var merged =
        duplicateResolutionService.resolve(
            reviewId,
            new ResolutionCommand(
                DuplicateResolutionAction.MERGE, survivor, absorbed, null, "Synthetic merge", key));

    assertThat(merged.status()).isEqualTo("RESOLVED");
    assertThat(merged.survivorProspectId()).isEqualTo(survivor);
    assertThat(merged.absorbedProspectId()).isEqualTo(absorbed);
    assertThat(
            jdbcTemplate.queryForMap(
                "SELECT status, eligibility, merged_into_id, archived_at FROM prospect WHERE id = ?",
                absorbed))
        .containsEntry("status", "DUPLICATE")
        .containsEntry("eligibility", "EXCLUDED")
        .containsEntry("merged_into_id", survivor)
        .doesNotContainEntry("archived_at", null);
    assertThat(count("prospect_note", survivor)).isEqualTo(1);
    assertThat(count("activity", survivor)).isEqualTo(1);
    assertThat(count("crm_task", survivor)).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM prospect_merge_map WHERE survivor_prospect_id = ? AND absorbed_prospect_id = ?",
                Integer.class,
                survivor,
                absorbed))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM contact_channel cc JOIN contact c ON c.id = cc.contact_id
                JOIN prospect p ON p.institution_id = c.institution_id
                WHERE p.id = ? AND c.deleted_at IS NULL
                """,
                Integer.class,
                survivor))
        .isEqualTo(2);

    var retried =
        duplicateResolutionService.resolve(
            reviewId,
            new ResolutionCommand(
                DuplicateResolutionAction.MERGE, survivor, absorbed, null, "Synthetic merge", key));
    assertThat(retried).isEqualTo(merged);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM prospect_merge_map WHERE duplicate_review_id = ?",
                Integer.class,
                reviewId))
        .isEqualTo(1);
  }

  @Test
  void resolvesLinkSeparateRejectAndDeferActions() {
    String suffix = UUID.randomUUID().toString();
    UUID existing = createProspect("Destino " + suffix, "destino." + suffix + "@test.invalid");

    UUID deferred = createReview(existing, null, 10);
    var deferredResult =
        duplicateResolutionService.resolve(
            deferred,
            command(DuplicateResolutionAction.DEFER, existing, null, null, "defer-" + suffix));
    assertThat(deferredResult.status()).isEqualTo("DEFERRED");
    assertThat(duplicateResolutionService.queue())
        .extracting(DuplicateResolutionService.ReviewView::id)
        .contains(deferred);

    UUID linked = createReview(existing, null, 11);
    duplicateResolutionService.resolve(
        linked,
        command(
            DuplicateResolutionAction.LINK_TO_EXISTING, existing, null, null, "link-" + suffix));
    assertRow(linked, "DUPLICATE", existing);

    UUID rejected = createReview(existing, null, 12);
    duplicateResolutionService.resolve(
        rejected,
        command(DuplicateResolutionAction.REJECT_ROW, null, null, null, "reject-" + suffix));
    assertRow(rejected, "REJECTED", null);

    UUID separate = createReview(existing, null, 13);
    var separateResult =
        duplicateResolutionService.resolve(
            separate,
            command(
                DuplicateResolutionAction.CREATE_SEPARATE,
                null,
                null,
                "Registro separado " + suffix,
                "separate-" + suffix));
    assertThat(separateResult.survivorProspectId()).isNotNull().isNotEqualTo(existing);
    assertRow(separate, "ACCEPTED", separateResult.survivorProspectId());

    UUID notDuplicate = createReview(existing, null, 14);
    var notDuplicateResult =
        duplicateResolutionService.resolve(
            notDuplicate,
            command(
                DuplicateResolutionAction.MARK_NOT_DUPLICATE,
                null,
                null,
                "No duplicado " + suffix,
                "not-duplicate-" + suffix));
    assertThat(notDuplicateResult.action()).isEqualTo("MARK_NOT_DUPLICATE");
    assertRow(notDuplicate, "ACCEPTED", notDuplicateResult.survivorProspectId());
  }

  @Test
  void rejectsCrossTenantOrMissingMergeTargetWithoutPartialWrites() {
    String suffix = UUID.randomUUID().toString();
    UUID survivor = createProspect("Rollback " + suffix, "rollback." + suffix + "@test.invalid");
    UUID review = createReview(survivor, null, 20);

    assertThatThrownBy(
            () ->
                duplicateResolutionService.resolve(
                    review,
                    command(
                        DuplicateResolutionAction.MERGE,
                        survivor,
                        UUID.randomUUID(),
                        null,
                        "invalid-merge-" + suffix)))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM duplicate_review WHERE id = ?", String.class, review))
        .isEqualTo("PENDING");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM prospect_merge_map WHERE duplicate_review_id = ?",
                Integer.class,
                review))
        .isZero();
  }

  private ResolutionCommand command(
      DuplicateResolutionAction action,
      UUID survivor,
      UUID absorbed,
      String separateName,
      String key) {
    return new ResolutionCommand(action, survivor, absorbed, separateName, "Synthetic test", key);
  }

  private UUID createProspect(String name, String email) {
    return prospectApplicationService
        .create(
            new CreateProspectCommand(
                name,
                "Danza",
                "Rosario",
                "Santa Fe",
                "Argentina",
                null,
                "Ana",
                "Directora",
                email,
                null,
                null,
                "DUP-" + UUID.randomUUID(),
                "fixture",
                "Synthetic duplicate evidence",
                100,
                2,
                60,
                null,
                null,
                Instant.parse("2026-07-21T12:00:00Z"),
                "duplicate-admin"))
        .id();
  }

  private UUID createReview(UUID existingProspectId, UUID rowProspectId, int rowNumber) {
    UUID jobId = UUID.randomUUID();
    UUID rowId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO import_job (
          id, version, created_at, updated_at, organization_id, file_name, file_sha256,
          idempotency_key, source_type, dry_run, status
        ) VALUES (?, 0, now(), now(), ?, 'synthetic.csv', ?, ?, 'CSV', FALSE, 'COMPLETED')
        """,
        jobId,
        principal.organizationId(),
        "0".repeat(64),
        "job-" + jobId);
    jdbcTemplate.update(
        """
        INSERT INTO import_row (
          id, version, created_at, updated_at, organization_id, import_job_id, source_sheet,
          row_number, raw_data, normalized_email, status, prospect_id
        ) VALUES (?, 0, now(), now(), ?, ?, 'Prospectos', ?, ?::jsonb::text, ?, 'REVIEW_REQUIRED', ?)
        """,
        rowId,
        principal.organizationId(),
        jobId,
        rowNumber,
        "{\"institucion\":\"Synthetic candidate\"}",
        "candidate-" + rowId + "@test.invalid",
        rowProspectId);
    jdbcTemplate.update(
        """
        INSERT INTO duplicate_review (
          id, version, created_at, updated_at, organization_id, import_row_id,
          existing_prospect_id, match_type, confidence, status, notes
        ) VALUES (?, 0, now(), now(), ?, ?, ?, 'NOMINAL_SIMILARITY', ?, 'PENDING', 'name and locality')
        """,
        reviewId,
        principal.organizationId(),
        rowId,
        existingProspectId,
        new BigDecimal("0.8500"));
    return reviewId;
  }

  private int count(String table, UUID prospectId) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM " + table + " WHERE prospect_id = ?", Integer.class, prospectId);
  }

  private void assertRow(UUID reviewId, String status, UUID prospectId) {
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            SELECT ir.status, ir.prospect_id FROM import_row ir
            JOIN duplicate_review dr ON dr.import_row_id = ir.id WHERE dr.id = ?
            """,
            reviewId);
    assertThat(row.get("status")).isEqualTo(status);
    assertThat(row.get("prospect_id")).isEqualTo(prospectId);
  }
}
