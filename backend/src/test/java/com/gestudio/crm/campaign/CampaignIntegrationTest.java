package com.gestudio.crm.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.campaign.CampaignService.AudienceFilter;
import com.gestudio.crm.campaign.CampaignService.CreateCampaignCommand;
import com.gestudio.crm.campaign.CampaignService.CreateTemplateCommand;
import com.gestudio.crm.campaign.CampaignService.SequenceStepCommand;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.common.UnprocessableEntityException;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.identity.IdentityService;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
      "security.bootstrap.username=campaign-admin",
      "security.bootstrap.password=campaign-password-1"
    })
@Testcontainers
class CampaignIntegrationTest {

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
  @Autowired private CampaignService campaignService;
  @Autowired private JdbcTemplate jdbcTemplate;

  private CrmPrincipal principal;

  @BeforeEach
  void authenticate() {
    principal = (CrmPrincipal) identityService.loadUserByUsername("campaign-admin");
    authenticate(principal);
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void freezesApprovesAndSimulatesAudienceWithoutSending() {
    String suffix = UUID.randomUUID().toString();
    String province = "Campaign " + suffix;
    UUID includedProspect =
        createProspect("Included " + suffix, "included-" + suffix + "@example.test", province);
    createProspect("No channel " + suffix, null, province);
    UUID excludedProspect =
        createProspect("Excluded " + suffix, "excluded-" + suffix + "@example.test", province);
    jdbcTemplate.update(
        """
        INSERT INTO exclusion (
          id, version, organization_id, channel_type, normalized_value, reason, created_at, updated_at
        ) VALUES (?, 0, ?, 'EMAIL', ?, 'MANUAL', now(), now())
        """,
        UUID.randomUUID(),
        principal.organizationId(),
        "excluded-" + suffix + "@example.test");

    var template = campaignService.createTemplate(templateCommand("Initial " + suffix));
    assertThat(template.versionNumber()).isEqualTo(1);
    assertThat(template.variables())
        .containsExactlyInAnyOrder("prospect.displayName", "contact.firstName", "campaign.name");

    var campaign =
        campaignService.createCampaign(
            new CreateCampaignCommand(
                "Campaign " + suffix,
                "Synthetic audience",
                "Validate simulation",
                CampaignChannel.EMAIL,
                template.versionId()));
    var steps =
        campaignService.replaceSequence(
            campaign.id(),
            campaign.version(),
            List.of(
                new SequenceStepCommand(SequenceStepType.EMAIL, Map.of()),
                new SequenceStepCommand(SequenceStepType.WAIT, Map.of("days", 2)),
                new SequenceStepCommand(
                    SequenceStepType.CONDITION, Map.of("condition", "REPLIED", "action", "STOP")),
                new SequenceStepCommand(SequenceStepType.STOP, Map.of())));
    assertThat(steps)
        .extracting(CampaignService.SequenceStepView::order)
        .containsExactly(1, 2, 3, 4);

    UUID campaignId = campaign.id();
    campaign =
        campaignService.campaigns().stream()
            .filter(item -> item.id().equals(campaignId))
            .findFirst()
            .orElseThrow();
    campaign =
        campaignService.freezeAudience(
            campaign.id(),
            campaign.version(),
            new AudienceFilter(null, "ELIGIBLE", null, null, province, null, true, false));
    assertThat(campaign.status()).isEqualTo(CampaignState.READY_FOR_REVIEW);
    assertThat(campaign.recipientCount()).isEqualTo(1);
    assertThat(campaign.excludedCount()).isEqualTo(2);
    assertThat(campaignService.audience(campaign.id()))
        .extracting(CampaignService.AudienceRecipientView::validationStatus)
        .containsExactlyInAnyOrder("VALID", "MISSING_CHANNEL", "EXCLUDED");

    campaign = campaignService.approve(campaign.id(), campaign.version());
    var simulation = campaignService.simulate(campaign.id(), "simulate-" + suffix);
    var retry = campaignService.simulate(campaign.id(), "simulate-" + suffix);
    assertThat(retry.id()).isEqualTo(simulation.id());
    assertThat(simulation.includedCount()).isEqualTo(1);
    assertThat(simulation.excludedCount()).isEqualTo(2);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM campaign_simulation_run WHERE campaign_id = ?",
                Integer.class,
                campaign.id()))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM campaign_simulation_result WHERE campaign_id = ? AND result = 'SIMULATED'",
                Integer.class,
                campaign.id()))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM activity WHERE prospect_id = ? AND activity_type = 'EMAIL_DRAFTED'",
                Integer.class,
                includedProspect))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM activity WHERE prospect_id = ?",
                Integer.class,
                excludedProspect))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT setting_value FROM system_setting WHERE organization_id = ? AND setting_key = 'sending.kill-switch'",
                String.class,
                principal.organizationId()))
        .isEqualTo("true");
  }

  @Test
  void rejectsUnsafeTemplatesMissingVariablesAndUnsafePersistentSettings() {
    String suffix = UUID.randomUUID().toString();
    assertThatThrownBy(
            () ->
                campaignService.createTemplate(
                    new CreateTemplateCommand(
                        "Unsafe " + suffix,
                        CampaignChannel.EMAIL,
                        "Subject",
                        "Text",
                        "<script>alert(1)</script>")))
        .isInstanceOf(UnprocessableEntityException.class);
    assertThatThrownBy(
            () ->
                campaignService.createTemplate(
                    new CreateTemplateCommand(
                        "Expression " + suffix,
                        CampaignChannel.EMAIL,
                        "{{system.execute}}",
                        "Text",
                        "<p>Safe</p>")))
        .isInstanceOf(UnprocessableEntityException.class);

    String province = "Blocked " + suffix;
    UUID prospectId =
        createProspect("Blocked " + suffix, "blocked-" + suffix + "@example.test", province);
    var template = campaignService.createTemplate(templateCommand("Blocked " + suffix));
    assertThatThrownBy(() -> campaignService.preview(template.versionId(), Map.of()))
        .isInstanceOf(UnprocessableEntityException.class)
        .hasMessageContaining("Missing template variable");
    var campaign =
        campaignService.createCampaign(
            new CreateCampaignCommand(
                "Blocked " + suffix, null, null, CampaignChannel.EMAIL, template.versionId()));
    campaign =
        campaignService.freezeAudience(
            campaign.id(),
            campaign.version(),
            new AudienceFilter(null, null, null, null, province, null, true, false));
    assertThat(campaign.recipientCount()).isPositive();
    campaign = campaignService.approve(campaign.id(), campaign.version());
    jdbcTemplate.update(
        "UPDATE system_setting SET setting_value = 'false' WHERE organization_id = ? AND setting_key = 'sending.kill-switch'",
        principal.organizationId());
    var approved = campaign;
    assertThatThrownBy(() -> campaignService.simulate(approved.id(), "unsafe-" + suffix))
        .isInstanceOf(UnprocessableEntityException.class)
        .hasMessageContaining("Persistent sending blockade");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM activity WHERE prospect_id = ?", Integer.class, prospectId))
        .isZero();
    jdbcTemplate.update(
        "UPDATE system_setting SET setting_value = 'true' WHERE organization_id = ? AND setting_key = 'sending.kill-switch'",
        principal.organizationId());
  }

  @Test
  void hidesCampaignsAndTemplatesAcrossOrganizations() {
    String suffix = UUID.randomUUID().toString();
    var template = campaignService.createTemplate(templateCommand("Tenant " + suffix));
    var campaign =
        campaignService.createCampaign(
            new CreateCampaignCommand(
                "Tenant " + suffix, null, null, CampaignChannel.EMAIL, template.versionId()));

    UUID otherOrganization = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, slug, active, timezone, currency, locale, created_at, updated_at) VALUES (?, 'Other tenant', ?, true, 'UTC', 'USD', 'es-AR', now(), now())",
        otherOrganization,
        "campaign-other-" + otherOrganization);
    authenticate(
        new CrmPrincipal(
            principal.userId(),
            otherOrganization,
            principal.username(),
            principal.displayName(),
            principal.password(),
            principal.role(),
            Set.copyOf(principal.permissions()),
            true,
            null));
    assertThat(campaignService.campaigns()).isEmpty();
    assertThat(campaignService.templates()).isEmpty();
    assertThatThrownBy(() -> campaignService.audience(campaign.id()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private CreateTemplateCommand templateCommand(String name) {
    return new CreateTemplateCommand(
        name,
        CampaignChannel.EMAIL,
        "Hola {{prospect.displayName}}",
        "Hola {{contact.firstName}}, campaña {{campaign.name}}",
        "<p>Hola <strong>{{contact.firstName}}</strong>, campaña {{campaign.name}}</p>");
  }

  private UUID createProspect(String name, String email, String province) {
    String suffix = UUID.randomUUID().toString();
    return prospectApplicationService
        .create(
            new CreateProspectCommand(
                name,
                "Education",
                "Rosario",
                province,
                "Argentina",
                "https://" + suffix + ".example.test",
                email == null ? null : "Contacto",
                email == null ? null : "Director",
                email,
                null,
                null,
                "CAMPAIGN-" + suffix,
                "TEST",
                "Synthetic",
                100,
                3,
                70,
                null,
                null,
                Instant.parse("2026-07-21T12:00:00Z"),
                principal.username()))
        .id();
  }

  private void authenticate(CrmPrincipal user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                user, user.password(), user.getAuthorities()));
  }
}
