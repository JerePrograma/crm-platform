package com.gestudio.crm.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.contact.ContactChannelRepository;
import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.contact.ContactRepository;
import com.gestudio.crm.exclusion.ExclusionApplicationService;
import com.gestudio.crm.exclusion.ExclusionReason;
import com.gestudio.crm.exclusion.ExclusionRepository;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.imports.ImportJobLifecycleService.ImportSummary;
import com.gestudio.crm.imports.ImportOperationsQueryService.RowSearchFilter;
import com.gestudio.crm.institution.InstitutionRepository;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.ProspectRepository;
import com.gestudio.crm.prospect.ProspectStatus;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
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

@SpringBootTest
@Testcontainers
class ProspectImportIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private ProspectImportService prospectImportService;
  @Autowired private ImportOperationsQueryService importOperationsQueryService;
  @Autowired private ProspectApplicationService prospectApplicationService;
  @Autowired private ExclusionApplicationService exclusionApplicationService;
  @Autowired private DuplicateReviewRepository duplicateReviewRepository;
  @Autowired private ImportRowRepository importRowRepository;
  @Autowired private ImportJobRepository importJobRepository;
  @Autowired private ContactChannelRepository contactChannelRepository;
  @Autowired private ContactRepository contactRepository;
  @Autowired private ProspectRepository prospectRepository;
  @Autowired private ExclusionRepository exclusionRepository;
  @Autowired private InstitutionRepository institutionRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanDatabase() {
    SecurityContextHolder.clearContext();
    jdbcTemplate.update("DELETE FROM audit_event");
    duplicateReviewRepository.deleteAll();
    importRowRepository.deleteAll();
    importJobRepository.deleteAll();
    contactChannelRepository.deleteAll();
    contactRepository.deleteAll();
    prospectRepository.deleteAll();
    exclusionRepository.deleteAll();
    institutionRepository.deleteAll();
  }

  @Test
  void importsOneHundredProspectsAndSixteenExclusionsIdempotently() {
    byte[] workbook = TestProspectWorkbookFactory.workbook(100, 16);

    ImportSummary first = prospectImportService.importFile("fixture-100.xlsx", workbook, false);
    ImportSummary repeated = prospectImportService.importFile("fixture-100.xlsx", workbook, false);

    assertThat(first.status()).isEqualTo(ImportJob.Status.COMPLETED);
    assertThat(first.totalRows()).isEqualTo(116);
    assertThat(first.acceptedRows()).isEqualTo(116);
    assertThat(first.excludedRows()).isZero();
    assertThat(first.rejectedRows()).isZero();
    assertThat(first.duplicateRows()).isZero();
    assertThat(first.reviewRows()).isZero();
    assertThat(repeated.id()).isEqualTo(first.id());
    assertThat(prospectRepository.count()).isEqualTo(100);
    assertThat(institutionRepository.count()).isEqualTo(100);
    assertThat(exclusionRepository.count()).isEqualTo(16);
    assertThat(importJobRepository.count()).isEqualTo(1);
    assertThat(importRowRepository.count()).isEqualTo(116);
  }

  @Test
  void pagesFiltersAndSearchesImportRowsWithinExplicitLimits() {
    byte[] workbook = TestProspectWorkbookFactory.workbook(105, 16);
    ImportSummary summary =
        prospectImportService.importFile("fixture-pagination-preview.xlsx", workbook, true);

    var firstPage =
        importOperationsQueryService.rows(
            summary.id(), new RowSearchFilter(null, null, null, 0, 25));
    assertThat(firstPage.totalElements()).isEqualTo(121);
    assertThat(firstPage.totalPages()).isEqualTo(5);
    assertThat(firstPage.number()).isZero();
    assertThat(firstPage.size()).isEqualTo(25);
    assertThat(firstPage.first()).isTrue();
    assertThat(firstPage.last()).isFalse();
    assertThat(firstPage.content()).hasSize(25);
    assertThat(firstPage.sourceSheets()).containsExactly("Exclusiones", "Prospectos");

    var secondPage =
        importOperationsQueryService.rows(
            summary.id(), new RowSearchFilter(null, null, null, 1, 25));
    assertThat(secondPage.number()).isEqualTo(1);
    assertThat(secondPage.content()).hasSize(25);
    assertThat(secondPage.content()).doesNotContainAnyElementsOf(firstPage.content());

    var exclusions =
        importOperationsQueryService.rows(
            summary.id(), new RowSearchFilter(null, "Exclusiones", null, 0, 25));
    assertThat(exclusions.totalElements()).isEqualTo(16);
    assertThat(exclusions.content())
        .allSatisfy(row -> assertThat(row.sourceSheet()).isEqualTo("Exclusiones"));

    var accepted =
        importOperationsQueryService.rows(
            summary.id(), new RowSearchFilter(ImportRow.Status.ACCEPTED, null, null, 0, 25));
    assertThat(accepted.totalElements()).isEqualTo(121);

    var search =
        importOperationsQueryService.rows(
            summary.id(), new RowSearchFilter(null, null, "contacto105@example.test", 0, 25));
    assertThat(search.totalElements()).isEqualTo(1);
    assertThat(search.content().getFirst().sourceSheet()).isEqualTo("Prospectos");
    assertThat(search.content().getFirst().rowNumber()).isEqualTo(106);

    var rowNumberSearch =
        importOperationsQueryService.rows(
            summary.id(), new RowSearchFilter(null, null, "000106", 0, 25));
    assertThat(rowNumberSearch.totalElements()).isEqualTo(1);
    assertThat(rowNumberSearch.content().getFirst().rowNumber()).isEqualTo(106);

    var capped =
        importOperationsQueryService.rows(
            summary.id(), new RowSearchFilter(null, null, null, -10, 500));
    assertThat(capped.number()).isZero();
    assertThat(capped.size()).isEqualTo(100);
    assertThat(capped.content()).hasSize(100);
    assertThat(capped.totalPages()).isEqualTo(2);
  }

  @Test
  void importRowPagesAreTenantIsolated() {
    byte[] workbook = TestProspectWorkbookFactory.workbook(1, 0);
    ImportSummary summary =
        prospectImportService.importFile("fixture-tenant-preview.xlsx", workbook, true);

    CrmPrincipal otherTenant =
        new CrmPrincipal(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "other-tenant",
            "Other Tenant",
            "unused",
            "SALES",
            Set.of("IMPORT_PREVIEW"),
            true,
            null);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                otherTenant, null, otherTenant.getAuthorities()));

    try {
      assertThatThrownBy(
              () ->
                  importOperationsQueryService.rows(
                      summary.id(), new RowSearchFilter(null, null, null, 0, 25)))
          .isInstanceOf(ResourceNotFoundException.class);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void dryRunPersistsEvidenceWithoutWritingProspectsOrExclusions() {
    byte[] workbook = TestProspectWorkbookFactory.workbook(2, 1);

    ImportSummary summary =
        prospectImportService.importFile("fixture-preview.xlsx", workbook, true);

    assertThat(summary.status()).isEqualTo(ImportJob.Status.COMPLETED);
    assertThat(summary.totalRows()).isEqualTo(3);
    assertThat(summary.acceptedRows()).isEqualTo(3);
    assertThat(summary.excludedRows()).isZero();
    assertThat(prospectRepository.count()).isZero();
    assertThat(institutionRepository.count()).isZero();
    assertThat(exclusionRepository.count()).isZero();
    assertThat(importRowRepository.count()).isEqualTo(3);
  }

  @Test
  void dryRunBlocksProspectRowsWithoutAUsableChannel() {
    byte[] csv =
        ("Institución,Localidad\n" + "Academia sin canal,Salta\n").getBytes(StandardCharsets.UTF_8);

    ImportSummary summary =
        prospectImportService.importFile("missing-channel-preview.csv", csv, true);

    ImportRow row = importRowRepository.findAll().getFirst();
    assertThat(summary.status()).isEqualTo(ImportJob.Status.COMPLETED);
    assertThat(summary.totalRows()).isEqualTo(1);
    assertThat(summary.acceptedRows()).isZero();
    assertThat(summary.excludedRows()).isEqualTo(1);
    assertThat(row.getStatus()).isEqualTo(ImportRow.Status.EXCLUDED);
    assertThat(prospectRepository.count()).isZero();
    assertThat(institutionRepository.count()).isZero();
  }

  @Test
  void dryRunMarksExcludedProspectRowsWithoutWritingDomainData() {
    exclusionApplicationService.create(
        ContactChannelType.EMAIL, "blocked@example.test", ExclusionReason.MANUAL);
    byte[] csv =
        ("Institución,Correo publicado,Localidad\n"
                + "Academia bloqueada,blocked@example.test,Salta\n")
            .getBytes(StandardCharsets.UTF_8);

    ImportSummary summary = prospectImportService.importFile("blocked-preview.csv", csv, true);

    ImportRow row = importRowRepository.findAll().getFirst();
    assertThat(summary.status()).isEqualTo(ImportJob.Status.COMPLETED);
    assertThat(summary.totalRows()).isEqualTo(1);
    assertThat(summary.acceptedRows()).isZero();
    assertThat(summary.excludedRows()).isEqualTo(1);
    assertThat(row.getStatus()).isEqualTo(ImportRow.Status.EXCLUDED);
    assertThat(prospectRepository.count()).isZero();
    assertThat(institutionRepository.count()).isZero();
    assertThat(exclusionRepository.count()).isEqualTo(1);
  }

  @Test
  void importedExclusionSuppressesAnExistingProspectAndWritesAuditEvidence() {
    var existing = prospectApplicationService.create(existingProspectCommand());
    byte[] workbook = TestProspectWorkbookFactory.workbook(0, 1);

    ImportSummary summary =
        prospectImportService.importFile("fixture-exclusion.xlsx", workbook, false);

    var reloaded = prospectRepository.findById(existing.id()).orElseThrow();
    assertThat(summary.status()).isEqualTo(ImportJob.Status.COMPLETED);
    assertThat(summary.acceptedRows()).isEqualTo(1);
    assertThat(summary.excludedRows()).isZero();
    assertThat(reloaded.isContactEligible()).isFalse();
    assertThat(reloaded.getStatus()).isEqualTo(ProspectStatus.DO_NOT_CONTACT);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_event WHERE action = 'EXCLUSION_CREATED'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void malformedEmailRejectsOnlyItsRowAndKeepsTheJobCompleted() {
    byte[] csv =
        ("Institución,Correo publicado,Localidad\n" + "Academia inválida,not-an-email,Córdoba\n")
            .getBytes(StandardCharsets.UTF_8);

    ImportSummary summary = prospectImportService.importFile("malformed-email.csv", csv, false);

    ImportRow row = importRowRepository.findAll().getFirst();
    assertThat(summary.status()).isEqualTo(ImportJob.Status.COMPLETED);
    assertThat(summary.totalRows()).isEqualTo(1);
    assertThat(summary.excludedRows()).isZero();
    assertThat(summary.rejectedRows()).isEqualTo(1);
    assertThat(row.getStatus()).isEqualTo(ImportRow.Status.REJECTED);
    assertThat(row.getNormalizedEmail()).isNull();
    assertThat(row.getErrorMessage()).contains("Invalid email address");
    assertThat(prospectRepository.count()).isZero();
  }

  private CreateProspectCommand existingProspectCommand() {
    return new CreateProspectCommand(
        "Academia con exclusión posterior",
        "Danza",
        "Mendoza",
        "Mendoza",
        "Argentina",
        "https://existing.example.test",
        "Administración",
        "Secretaría",
        "excluido1@example.test",
        null,
        null,
        "EXISTING-EXCLUSION-1",
        "fixture",
        "Non-production test evidence",
        40,
        1,
        60,
        null,
        null,
        null,
        "test-owner");
  }
}
