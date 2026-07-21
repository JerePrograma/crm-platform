package com.gestudio.crm.exclusion;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.audit.AuditQueryService;
import com.gestudio.crm.contact.ContactChannelRepository;
import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.contact.ContactRepository;
import com.gestudio.crm.institution.InstitutionRepository;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.ProspectRepository;
import com.gestudio.crm.prospect.ProspectStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Testcontainers
class ExclusionIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private ExclusionApplicationService exclusionApplicationService;
  @Autowired private ProspectApplicationService prospectApplicationService;
  @Autowired private AuditEventWriter auditEventWriter;
  @Autowired private AuditQueryService auditQueryService;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ContactChannelRepository contactChannelRepository;
  @Autowired private ContactRepository contactRepository;
  @Autowired private ProspectRepository prospectRepository;
  @Autowired private ExclusionRepository exclusionRepository;
  @Autowired private InstitutionRepository institutionRepository;

  @BeforeEach
  void cleanDatabase() {
    jdbcTemplate.update("DELETE FROM audit_event");
    contactChannelRepository.deleteAll();
    contactRepository.deleteAll();
    prospectRepository.deleteAll();
    exclusionRepository.deleteAll();
    institutionRepository.deleteAll();
  }

  @Test
  void newExclusionMarksAnExistingProspectAsDoNotContact() {
    var prospect = prospectApplicationService.create(command());

    var exclusion =
        exclusionApplicationService.create(
            ContactChannelType.EMAIL, " ADMIN@EXAMPLE.TEST ", ExclusionReason.MANUAL);

    var reloaded = prospectRepository.findById(prospect.id()).orElseThrow();
    assertThat(exclusion.normalizedValue()).isEqualTo("admin@example.test");
    assertThat(reloaded.isContactEligible()).isFalse();
    assertThat(reloaded.getStatus()).isEqualTo(ProspectStatus.DO_NOT_CONTACT);
    assertThat(auditQueryService.recent(10))
        .extracting(AuditQueryService.AuditEventView::action)
        .contains("PROSPECT_CREATED", "EXCLUSION_CREATED");
    assertThat(auditQueryService.recent(10))
        .extracting(AuditQueryService.AuditEventView::payload)
        .noneMatch(payload -> payload.contains("admin@example.test"));
  }

  @Test
  void springJsonMapperSerializesAndPersistsAuditDetails() throws JacksonException {
    UUID entityId = UUID.fromString("3a9cfeb1-2716-4b32-b122-f1e3923e4796");
    Instant instant = Instant.parse("2026-07-21T15:00:00Z");
    OffsetDateTime offsetDateTime = OffsetDateTime.parse("2026-07-21T12:00:00-03:00");
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("entityId", entityId);
    details.put("instant", instant);
    details.put("offsetDateTime", offsetDateTime);
    details.put("channelType", ContactChannelType.EMAIL);
    details.put("optional", null);

    UUID auditId = auditEventWriter.record("AUDIT_FIXTURE", "Fixture", entityId, details);
    String payload =
        jdbcTemplate.queryForObject(
            "SELECT payload::text FROM audit_event WHERE id = ?", String.class, auditId);
    JsonNode json = objectMapper.readTree(payload);

    assertThat(json.get("entityId").asText()).isEqualTo(entityId.toString());
    assertThat(Instant.parse(json.get("instant").asText())).isEqualTo(instant);
    assertThat(OffsetDateTime.parse(json.get("offsetDateTime").asText()).toInstant())
        .isEqualTo(offsetDateTime.toInstant());
    assertThat(json.get("channelType").asText()).isEqualTo("EMAIL");
    assertThat(json.get("optional").isNull()).isTrue();
  }

  private CreateProspectCommand command() {
    return new CreateProspectCommand(
        "Academia Fixture",
        "Danza",
        "Rosario",
        "Santa Fe",
        "Argentina",
        null,
        "Administración",
        "Secretaría",
        "admin@example.test",
        null,
        null,
        "FIXTURE-EXCLUSION-1",
        "fixture",
        "Non-production test data",
        50,
        1,
        50,
        null,
        null,
        null,
        null);
  }
}
