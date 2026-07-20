package com.gestudio.crm.imports;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestudio.crm.contact.ContactChannelRepository;
import com.gestudio.crm.contact.ContactRepository;
import com.gestudio.crm.exclusion.ExclusionRepository;
import com.gestudio.crm.imports.ImportJob.SourceType;
import com.gestudio.crm.imports.ImportJobLifecycleService.RowOutcome;
import com.gestudio.crm.imports.ProspectDeduplicationService.Kind;
import com.gestudio.crm.imports.ProspectImportFileParser.ProspectCandidate;
import com.gestudio.crm.institution.InstitutionRepository;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.ProspectRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ProspectDeduplicationIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private ProspectDeduplicationService deduplicationService;
  @Autowired private ProspectImportRowProcessor rowProcessor;
  @Autowired private ImportJobLifecycleService importJobLifecycleService;
  @Autowired private ProspectApplicationService prospectApplicationService;
  @Autowired private DuplicateReviewRepository duplicateReviewRepository;
  @Autowired private ImportRowRepository importRowRepository;
  @Autowired private ImportJobRepository importJobRepository;
  @Autowired private ContactChannelRepository contactChannelRepository;
  @Autowired private ContactRepository contactRepository;
  @Autowired private ProspectRepository prospectRepository;
  @Autowired private ExclusionRepository exclusionRepository;
  @Autowired private InstitutionRepository institutionRepository;

  @BeforeEach
  void cleanDatabase() {
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
  void ambiguousNameInSameLocalityRequiresHumanReview() {
    prospectApplicationService.create(existingCommand());

    ProspectDeduplicationService.DeduplicationOutcome outcome =
        deduplicationService.evaluate(ambiguousCandidate());

    assertThat(outcome.kind()).isEqualTo(Kind.REVIEW_REQUIRED);
    assertThat(outcome.existingProspect()).isNotNull();
    assertThat(prospectRepository.count()).isEqualTo(1);
    assertThat(institutionRepository.count()).isEqualTo(1);
  }

  @Test
  void previewPersistsAmbiguousReviewEvidenceWithoutCreatingAnotherProspect() {
    prospectApplicationService.create(existingCommand());
    var job =
        importJobLifecycleService.start(
            "ambiguous-preview.xlsx",
            "0".repeat(64),
            "preview-ambiguous-fixture",
            SourceType.XLSX,
            true);

    RowOutcome outcome = rowProcessor.processProspect(job.jobId(), ambiguousCandidate(), true);

    assertThat(outcome).isEqualTo(RowOutcome.REVIEW_REQUIRED);
    assertThat(duplicateReviewRepository.count()).isEqualTo(1);
    assertThat(importRowRepository.count()).isEqualTo(1);
    assertThat(prospectRepository.count()).isEqualTo(1);
    assertThat(institutionRepository.count()).isEqualTo(1);
  }

  @Test
  @Transactional
  void exactDuplicateRowRetainsTheExistingProspectReference() {
    var existing = prospectApplicationService.create(existingCommand());
    var job =
        importJobLifecycleService.start(
            "exact-duplicate.xlsx",
            "1".repeat(64),
            "exact-duplicate-fixture",
            SourceType.XLSX,
            true);

    RowOutcome outcome = rowProcessor.processProspect(job.jobId(), exactDuplicateCandidate(), true);

    ImportRow row = importRowRepository.findAll().getFirst();
    assertThat(outcome).isEqualTo(RowOutcome.DUPLICATE);
    assertThat(row.getStatus()).isEqualTo(ImportRow.Status.DUPLICATE);
    assertThat(row.getProspect()).isNotNull();
    assertThat(row.getProspect().getId()).isEqualTo(existing.id());
    assertThat(prospectRepository.count()).isEqualTo(1);
  }

  private ProspectCandidate ambiguousCandidate() {
    return new ProspectCandidate(
        2,
        Map.of("institucion", "Estudio Auroa"),
        "NEW-SOURCE",
        "Estudio Auroa",
        "Junín",
        "Buenos Aires",
        "Academia de danza",
        null,
        null,
        "otro@example.test",
        null,
        "fixture",
        null,
        null,
        2,
        "Variación nominal ficticia");
  }

  private ProspectCandidate exactDuplicateCandidate() {
    return new ProspectCandidate(
        3,
        Map.of("institucion", "Estudio Aurora"),
        "EXISTING-SOURCE",
        "Estudio Aurora",
        "Junín",
        "Buenos Aires",
        "Academia de danza",
        null,
        null,
        "otro-exacto@example.test",
        null,
        "fixture",
        null,
        null,
        1,
        "Coincidencia exacta ficticia");
  }

  private CreateProspectCommand existingCommand() {
    return new CreateProspectCommand(
        "Estudio Aurora",
        "Academia de danza",
        "Junín",
        "Buenos Aires",
        "Argentina",
        null,
        null,
        null,
        "aurora@example.test",
        null,
        null,
        "EXISTING-SOURCE",
        "fixture",
        "Existing fictitious record",
        null,
        1,
        null,
        null,
        null,
        null,
        null);
  }
}
