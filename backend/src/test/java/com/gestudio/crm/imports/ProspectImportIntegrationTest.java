package com.gestudio.crm.imports;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestudio.crm.contact.ContactChannelRepository;
import com.gestudio.crm.contact.ContactRepository;
import com.gestudio.crm.exclusion.ExclusionRepository;
import com.gestudio.crm.imports.ImportJobLifecycleService.ImportSummary;
import com.gestudio.crm.institution.InstitutionRepository;
import com.gestudio.crm.prospect.ProspectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
  void importsOneHundredProspectsAndSixteenExclusionsIdempotently() {
    byte[] workbook = TestProspectWorkbookFactory.workbook(100, 16);

    ImportSummary first = prospectImportService.importFile("fixture-100.xlsx", workbook, false);
    ImportSummary repeated = prospectImportService.importFile("fixture-100.xlsx", workbook, false);

    assertThat(first.status()).isEqualTo(ImportJob.Status.COMPLETED);
    assertThat(first.totalRows()).isEqualTo(116);
    assertThat(first.acceptedRows()).isEqualTo(116);
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
    assertThat(prospectRepository.count()).isZero();
    assertThat(institutionRepository.count()).isZero();
    assertThat(exclusionRepository.count()).isZero();
    assertThat(importRowRepository.count()).isEqualTo(3);
  }
}
