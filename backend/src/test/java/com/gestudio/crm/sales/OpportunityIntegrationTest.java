package com.gestudio.crm.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.activity.TaskPriority;
import com.gestudio.crm.activity.TimelineService;
import com.gestudio.crm.activity.TimelineService.CreateTaskCommand;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.common.UnprocessableEntityException;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.identity.IdentityService;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.sales.OpportunityService.CreateOpportunityCommand;
import com.gestudio.crm.sales.OpportunityService.TransitionCommand;
import com.gestudio.crm.sales.OpportunityService.UpdateOpportunityCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
      "security.bootstrap.username=sales-admin",
      "security.bootstrap.password=sales-password-1"
    })
@Testcontainers
class OpportunityIntegrationTest {

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
  @Autowired private OpportunityService opportunityService;
  @Autowired private TimelineService timelineService;
  @Autowired private JdbcTemplate jdbcTemplate;

  private CrmPrincipal principal;

  @BeforeEach
  void authenticate() {
    principal = (CrmPrincipal) identityService.loadUserByUsername("sales-admin");
    authenticate(principal);
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void advancesPipelineToWonAndConvertsTheProspect() {
    UUID prospectId = createProspect("Ganada");
    timelineService.createTask(
        prospectId,
        new CreateTaskCommand(
            principal.userId(),
            "Seguimiento de prospección",
            null,
            Instant.parse("2026-08-01T12:00:00Z"),
            TaskPriority.HIGH,
            "PROSPECTING",
            null));

    var opportunity =
        opportunityService.create(
            createCommand(prospectId, "Licencias anuales", new BigDecimal("120000.00"), true));
    assertThat(opportunity.stage()).isEqualTo(OpportunityStage.QUALIFICATION);
    assertThat(opportunity.primaryActive()).isTrue();

    opportunity = move(opportunity, OpportunityStage.DISCOVERY, null);
    opportunity = move(opportunity, OpportunityStage.DEMO, null);
    opportunity = move(opportunity, OpportunityStage.PROPOSAL, null);
    opportunity = move(opportunity, OpportunityStage.NEGOTIATION, null);
    opportunity = move(opportunity, OpportunityStage.WON, "Contrato aprobado");

    assertThat(opportunity.probability()).isEqualTo(100);
    assertThat(opportunity.actualCloseDate()).isNotNull();
    assertThat(opportunity.wonReason()).isEqualTo("Contrato aprobado");
    assertThat(opportunity.primaryActive()).isFalse();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM prospect WHERE id = ?", String.class, prospectId))
        .isEqualTo("CUSTOMER");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM crm_task WHERE prospect_id = ?", String.class, prospectId))
        .isEqualTo("CANCELLED");
    assertThat(opportunityService.history(opportunity.id())).hasSize(6);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM activity WHERE prospect_id = ? AND metadata->>'opportunityId' = ?",
                Integer.class,
                prospectId,
                opportunity.id().toString()))
        .isEqualTo(6);
  }

  @Test
  void enforcesLossReasonOptimisticLockAndSinglePrimaryOpportunity() {
    var metricsBefore = opportunityService.metrics();
    UUID prospectId = createProspect("Conflicto");
    var first =
        opportunityService.create(
            createCommand(prospectId, "Primaria", new BigDecimal("100.00"), true));
    var second =
        opportunityService.create(
            createCommand(prospectId, "Secundaria", new BigDecimal("300.00"), true));
    assertThat(opportunityService.get(first.id()).primaryActive()).isFalse();
    assertThat(second.primaryActive()).isTrue();

    var updated =
        opportunityService.update(
            second.id(),
            new UpdateOpportunityCommand(
                second.version(),
                second.name(),
                principal.userId(),
                new BigDecimal("400.00"),
                "ARS",
                20,
                LocalDate.parse("2026-12-01"),
                "MANUAL",
                true));
    assertThat(updated.estimatedValue()).isEqualByComparingTo("400.00");
    assertThatThrownBy(
            () ->
                opportunityService.update(
                    second.id(),
                    new UpdateOpportunityCommand(
                        second.version(),
                        second.name(),
                        principal.userId(),
                        BigDecimal.ONE,
                        "ARS",
                        10,
                        null,
                        null,
                        true)))
        .isInstanceOf(OptimisticConflictException.class);
    assertThatThrownBy(
            () ->
                opportunityService.transition(
                    first.id(),
                    new TransitionCommand(first.version() + 1, OpportunityStage.LOST, null, null)))
        .isInstanceOf(IllegalArgumentException.class);

    var lost =
        opportunityService.transition(
            first.id(),
            new TransitionCommand(
                opportunityService.get(first.id()).version(),
                OpportunityStage.LOST,
                "NO_BUDGET",
                "Synthetic loss"));
    assertThat(lost.lostReason()).isEqualTo("NO_BUDGET");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM prospect WHERE id = ?", String.class, prospectId))
        .isEqualTo("NEW");
    assertThat(opportunityService.metrics().activeCount())
        .isEqualTo(metricsBefore.activeCount() + 1);
    assertThat(opportunityService.metrics().weightedValue())
        .isEqualByComparingTo(metricsBefore.weightedValue().add(new BigDecimal("80.00")));
  }

  @Test
  void rejectsInvalidTransitionsAndHidesOtherOrganizations() {
    UUID prospectId = createProspect("Tenant");
    var opportunity =
        opportunityService.create(
            createCommand(prospectId, "Aislada", new BigDecimal("50.00"), true));
    assertThatThrownBy(
            () ->
                opportunityService.transition(
                    opportunity.id(),
                    new TransitionCommand(
                        opportunity.version(), OpportunityStage.WON, "Won too early", null)))
        .isInstanceOf(UnprocessableEntityException.class);

    UUID otherOrganization = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, slug, active, timezone, currency, locale, created_at, updated_at) VALUES (?, 'Other tenant', ?, true, 'UTC', 'USD', 'es-AR', now(), now())",
        otherOrganization,
        "other-" + otherOrganization);
    authenticate(
        new CrmPrincipal(
            principal.userId(),
            otherOrganization,
            principal.username(),
            principal.displayName(),
            principal.password(),
            principal.role(),
            principal.permissions(),
            true,
            null));
    assertThat(opportunityService.list(null, null, null)).isEmpty();
    assertThatThrownBy(() -> opportunityService.get(opportunity.id()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private OpportunityService.OpportunityView move(
      OpportunityService.OpportunityView opportunity, OpportunityStage stage, String reason) {
    return opportunityService.transition(
        opportunity.id(),
        new TransitionCommand(opportunity.version(), stage, reason, "Synthetic transition"));
  }

  private CreateOpportunityCommand createCommand(
      UUID prospectId, String name, BigDecimal value, boolean primary) {
    return new CreateOpportunityCommand(
        prospectId,
        name,
        principal.userId(),
        value,
        "ARS",
        10,
        LocalDate.parse("2026-12-01"),
        "MANUAL",
        primary);
  }

  private UUID createProspect(String label) {
    String suffix = UUID.randomUUID().toString();
    return prospectApplicationService
        .create(
            new CreateProspectCommand(
                "Prospecto " + label + " " + suffix,
                "Danza",
                "Rosario",
                "Santa Fe",
                "Argentina",
                null,
                null,
                null,
                null,
                null,
                null,
                "SALES-" + suffix,
                "fixture",
                "Synthetic opportunity evidence",
                100,
                2,
                60,
                null,
                null,
                Instant.parse("2026-07-21T12:00:00Z"),
                "sales-admin"))
        .id();
  }

  private void authenticate(CrmPrincipal crmPrincipal) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                crmPrincipal, crmPrincipal.password(), crmPrincipal.getAuthorities()));
  }
}
