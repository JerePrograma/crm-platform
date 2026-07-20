package com.gestudio.crm.imports;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestudio.crm.contact.ContactChannelRepository;
import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.contact.ContactRepository;
import com.gestudio.crm.exclusion.ExclusionApplicationService;
import com.gestudio.crm.exclusion.ExclusionReason;
import com.gestudio.crm.exclusion.ExclusionRepository;
import com.gestudio.crm.imports.ImportJobLifecycleService.ImportSummary;
import com.gestudio.crm.institution.InstitutionRepository;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.ProspectRepository;
import com.gestudio.crm.prospect.ProspectStatus;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ProspectImportIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private ProspectImportService prospectImportService;
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
  void dryRunPersistsEvidenceWithoutWritingProspectsOrExclusions() {
    byte[] workbook = TestProspectWorkbookFactory.workbook(2, 1);

    ImportSummary summary = prospectImportService.importFile("fixture-preview.xlsx", workbook, true);

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

    ImportSummary summary = prospectImportService.importFile("fixture-exclusion.xlsx", workbook, false);

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
        ("Institución,Correo publicado,Localidad\n"
                + "Academia inválida,not-an-email,Córdoba\n")
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
