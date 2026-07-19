package com.gestudio.crm.imports;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestudio.crm.contact.ContactChannelRepository;
import com.gestudio.crm.contact.ContactRepository;
import com.gestudio.crm.exclusion.ExclusionRepository;
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
  @Autowired private ProspectApplicationService prospectApplicationService;
  @Autowired private ContactChannelRepository contactChannelRepository;
  @Autowired private ContactRepository contactRepository;
  @Autowired private ProspectRepository prospectRepository;
  @Autowired private ExclusionRepository exclusionRepository;
  @Autowired private InstitutionRepository institutionRepository;

  @BeforeEach
  void cleanDatabase() {
    contactChannelRepository.deleteAll();
    contactRepository.deleteAll();
    prospectRepository.deleteAll();
    exclusionRepository.deleteAll();
    institutionRepository.deleteAll();
  }

  @Test
  void ambiguousNameInSameLocalityRequiresHumanReview() {
    prospectApplicationService.create(existingCommand());

    ProspectCandidate candidate =
        new ProspectCandidate(
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

    ProspectDeduplicationService.DeduplicationOutcome outcome =
        deduplicationService.evaluate(candidate);

    assertThat(outcome.kind()).isEqualTo(Kind.REVIEW_REQUIRED);
    assertThat(outcome.existingProspect()).isNotNull();
    assertThat(prospectRepository.count()).isEqualTo(1);
    assertThat(institutionRepository.count()).isEqualTo(1);
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
