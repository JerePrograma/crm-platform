package com.gestudio.crm.prospect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.common.DuplicateResourceException;
import com.gestudio.crm.contact.ContactChannelRepository;
import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.contact.ContactRepository;
import com.gestudio.crm.exclusion.Exclusion;
import com.gestudio.crm.exclusion.ExclusionReason;
import com.gestudio.crm.exclusion.ExclusionRepository;
import com.gestudio.crm.institution.InstitutionRepository;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.ProspectApplicationService.ProspectView;
import java.time.Instant;
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
class ProspectPersistenceIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private ProspectApplicationService prospectApplicationService;
  @Autowired private ProspectRepository prospectRepository;
  @Autowired private InstitutionRepository institutionRepository;
  @Autowired private ContactRepository contactRepository;
  @Autowired private ContactChannelRepository contactChannelRepository;
  @Autowired private ExclusionRepository exclusionRepository;

  @BeforeEach
  void cleanDatabase() {
    contactChannelRepository.deleteAll();
    contactRepository.deleteAll();
    prospectRepository.deleteAll();
    exclusionRepository.deleteAll();
    institutionRepository.deleteAll();
  }

  @Test
  void persistsNormalizedInstitutionContactAndProspect() {
    ProspectView created = prospectApplicationService.create(command("SRC-001", "Admin@Aurora.TEST"));

    assertThat(created.status()).isEqualTo(ProspectStatus.NEW);
    assertThat(created.contactEligible()).isTrue();
    assertThat(prospectRepository.count()).isEqualTo(1);
    assertThat(institutionRepository.findAll())
        .singleElement()
        .satisfies(
            institution -> {
              assertThat(institution.getNormalizedName()).isEqualTo("estudio aurora");
              assertThat(institution.getNormalizedLocality()).isEqualTo("cordoba");
              assertThat(institution.getWebsiteDomain()).isEqualTo("aurora.test");
            });
    assertThat(
            contactChannelRepository.findByTypeAndNormalizedValue(
                ContactChannelType.EMAIL, "admin@aurora.test"))
        .isPresent();
  }

  @Test
  void exclusionDominatesEligibilityAndCommercialStatus() {
    exclusionRepository.save(
        Exclusion.create(
            ContactChannelType.EMAIL, "admin@aurora.test", ExclusionReason.MANUAL));

    ProspectView created = prospectApplicationService.create(command("SRC-002", "Admin@Aurora.TEST"));

    assertThat(created.contactEligible()).isFalse();
    assertThat(created.status()).isEqualTo(ProspectStatus.DO_NOT_CONTACT);
  }

  @Test
  void repeatedExternalSourceIdIsRejected() {
    prospectApplicationService.create(command("SRC-003", "first@aurora.test"));

    assertThatThrownBy(
            () -> prospectApplicationService.create(command("SRC-003", "second@aurora.test")))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("external source id");
  }

  private CreateProspectCommand command(String externalSourceId, String email) {
    return new CreateProspectCommand(
        "Estúdio Aurora",
        "Danza",
        "Córdoba",
        "Córdoba",
        "Argentina",
        "https://www.aurora.test/contacto",
        "Administración",
        "Secretaría",
        email,
        "+54 351 555 0101",
        null,
        externalSourceId,
        "fixture",
        "Public test evidence",
        80,
        1,
        75,
        "Planillas y WhatsApp",
        "Seguimiento manual de cuotas",
        Instant.parse("2026-07-01T12:00:00Z"),
        "test-owner");
  }
}
