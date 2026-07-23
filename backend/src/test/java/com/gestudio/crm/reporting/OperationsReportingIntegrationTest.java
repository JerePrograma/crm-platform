package com.gestudio.crm.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.identity.IdentityService;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.ProspectOperationsService;
import com.gestudio.crm.prospect.ProspectOperationsService.SearchFilter;
import com.gestudio.crm.prospect.TagService;
import com.gestudio.crm.settings.OrganizationSettingsService;
import com.gestudio.crm.settings.OrganizationSettingsService.UpdateCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
      "security.bootstrap.username=report-admin",
      "security.bootstrap.password=report-password-1"
    })
@Testcontainers
class OperationsReportingIntegrationTest {

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
  @Autowired private ProspectOperationsService prospectOperationsService;
  @Autowired private OrganizationSettingsService settingsService;
  @Autowired private TagService tagService;
  @Autowired private ReportingService reportingService;
  @Autowired private JdbcTemplate jdbcTemplate;

  private CrmPrincipal principal;

  @BeforeEach
  void authenticate() {
    principal = (CrmPrincipal) identityService.loadUserByUsername("report-admin");
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
  void protectsSettingsAndTagsWithTenantScopedAudit() {
    UUID prospectId = createProspect("Tagged search target", "TAGGED-" + UUID.randomUUID());
    var original = settingsService.get();
    var updated =
        settingsService.update(
            new UpdateCommand(
                original.version(),
                "Gestudio Synthetic",
                "America/Argentina/Buenos_Aires",
                "ARS",
                "es-AR",
                "#1D4ED8",
                5,
                "08:30",
                "17:30",
                List.of(1, 2, 3, 4, 5),
                true,
                false,
                25,
                false));

    assertThat(updated.sendingOverrideRejected()).isTrue();
    assertThat(updated.campaignDailyLimit()).isZero();
    assertThat(updated.sending().environmentEnabled()).isFalse();
    assertThat(updated.sending().environmentDryRun()).isTrue();
    assertThat(updated.sending().environmentDailyLimit()).isZero();
    assertThat(updated.sending().environmentKillSwitch()).isTrue();
    assertThat(updated.sending().databaseKillSwitch()).isEqualTo("true");

    var tag = tagService.create("Prioridad Norte", "#2563EB");
    tagService.assign(tag.id(), List.of(prospectId, prospectId));
    assertThat(tagService.forProspect(prospectId))
        .extracting(TagService.TagView::id)
        .contains(tag.id());
    assertThat(
            prospectOperationsService
                .search(
                    new SearchFilter(
                        "Prioridad Norte", null, null, false, 0, 20, "relevance", "desc"))
                .content())
        .extracting(ProspectOperationsService.OperationalProspectView::id)
        .contains(prospectId);

    UUID foreignOrganization = UUID.randomUUID();
    UUID foreignInstitution = UUID.randomUUID();
    UUID foreignProspect = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO organization (id, version, created_at, updated_at, slug, name) VALUES (?, 0, now(), now(), ?, 'Foreign organization')",
        foreignOrganization,
        "foreign-" + foreignOrganization);
    jdbcTemplate.update(
        "INSERT INTO institution (id, version, created_at, updated_at, name, normalized_name, normalized_locality, organization_id) VALUES (?, 0, now(), now(), 'Foreign secret', 'foreign secret', 'remote', ?)",
        foreignInstitution,
        foreignOrganization);
    jdbcTemplate.update(
        "INSERT INTO prospect (id, version, created_at, updated_at, institution_id, status, contact_eligible, eligibility, organization_id) VALUES (?, 0, now(), now(), ?, 'NEW', TRUE, 'ELIGIBLE', ?)",
        foreignProspect,
        foreignInstitution,
        foreignOrganization);

    assertThat(
            prospectOperationsService
                .search(
                    new SearchFilter(
                        "Foreign secret", null, null, false, 0, 20, "relevance", "desc"))
                .content())
        .isEmpty();
    assertThatThrownBy(() -> tagService.assign(tag.id(), List.of(foreignProspect)))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_event WHERE organization_id = ? AND action IN ('ORGANIZATION_SETTINGS_UPDATED_WITH_SENDING_OVERRIDE_REJECTED', 'TAG_CREATED', 'TAG_ASSIGNED_BULK')",
                Integer.class,
                principal.organizationId()))
        .isEqualTo(3);
  }

  @Test
  void reportsExactTenantAggregatesWithoutMixingCurrenciesAndExportsSafeCsv() {
    UUID first = createProspect("=Formula academy", "REPORT-A-" + UUID.randomUUID());
    UUID second = createProspect("Report academy B", "REPORT-B-" + UUID.randomUUID());
    insertOpportunity(first, "ARS", new BigDecimal("1000.00"), 50);
    insertOpportunity(second, "USD", new BigDecimal("200.00"), 25);

    var report = reportingService.dashboard(null, null);
    assertThat(report.prospectsByStatus().get("NEW")).isGreaterThanOrEqualTo(2);
    assertThat(report.opportunityValues())
        .extracting(ReportingService.CurrencyTotal::currency)
        .contains("ARS", "USD");
    assertThat(report.opportunityValues())
        .filteredOn(total -> total.currency().equals("ARS"))
        .singleElement()
        .satisfies(
            total -> {
              assertThat(total.totalValue()).isEqualByComparingTo("1000.00");
              assertThat(total.weightedValue()).isEqualByComparingTo("500.00");
            });
    assertThat(reportingService.csv(null, null))
        .startsWith("section,metric,value,currency")
        .doesNotContain("=Formula academy");
  }

  private UUID createProspect(String name, String externalId) {
    return prospectApplicationService
        .create(
            new CreateProspectCommand(
                name,
                "Academy",
                "Rosario",
                "Santa Fe",
                "Argentina",
                null,
                "Report contact",
                "Administration",
                externalId + "@example.test",
                null,
                null,
                externalId,
                "SYNTHETIC",
                "Synthetic test evidence",
                100,
                2,
                60,
                null,
                null,
                Instant.parse("2026-07-22T12:00:00Z"),
                principal.username()))
        .id();
  }

  private void insertOpportunity(
      UUID prospectId, String currency, BigDecimal value, int probability) {
    jdbcTemplate.update(
        """
        INSERT INTO opportunity (
          id, version, organization_id, prospect_id, name, owner_user_id, stage,
          estimated_value, currency, probability, primary_active, stage_changed_at,
          created_by, updated_by, created_at, updated_at
        ) VALUES (?, 0, ?, ?, 'Synthetic opportunity', ?, 'QUALIFICATION', ?, ?, ?, FALSE,
          now(), ?, ?, now(), now())
        """,
        UUID.randomUUID(),
        principal.organizationId(),
        prospectId,
        principal.userId(),
        value,
        currency,
        probability,
        principal.userId(),
        principal.userId());
  }
}
