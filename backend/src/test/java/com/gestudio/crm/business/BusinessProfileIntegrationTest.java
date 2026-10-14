package com.gestudio.crm.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.campaign.CampaignChannel;
import com.gestudio.crm.campaign.CampaignService;
import com.gestudio.crm.campaign.CampaignService.AudienceFilter;
import com.gestudio.crm.campaign.CampaignService.CreateCampaignCommand;
import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.common.TenantIds;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.identity.IdentityService;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.TagService;
import com.gestudio.crm.security.CurrentActor;
import com.gestudio.crm.security.SecurityBootstrapProperties;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "crm.business.profile=SCALARIS",
      "crm.business.marketing-username=scalaris-marketing",
      "crm.business.marketing-password=synthetic-marketing-1",
      "security.bootstrap.username=scalaris-admin",
      "security.bootstrap.password=synthetic-admin-1"
    })
@AutoConfigureMockMvc
class BusinessProfileIntegrationTest {

  private static PostgreSQLContainer<?> postgres;

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    String localUrl = System.getenv("CRM_TEST_DATABASE_URL");
    if (localUrl != null && !localUrl.isBlank()) {
      if (!localUrl.startsWith("jdbc:postgresql://127.0.0.1:")
          || !localUrl.matches(
              "jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/scalaris_crm_test[a-z0-9_]*")) {
        throw new IllegalArgumentException(
            "CRM_TEST_DATABASE_URL must name an isolated loopback scalaris_crm_test database");
      }
      registry.add("spring.datasource.url", () -> localUrl);
      registry.add(
          "spring.datasource.username", () -> requiredEnvironment("CRM_TEST_DATABASE_USER"));
      registry.add(
          "spring.datasource.password", () -> requiredEnvironment("CRM_TEST_DATABASE_PASSWORD"));
    } else {
      postgres = new PostgreSQLContainer<>("postgres:17-alpine");
      postgres.start();
      registry.add("spring.datasource.url", postgres::getJdbcUrl);
      registry.add("spring.datasource.username", postgres::getUsername);
      registry.add("spring.datasource.password", postgres::getPassword);
    }
  }

  @AfterAll
  static void stopSyntheticContainer() {
    if (postgres != null) {
      postgres.stop();
    }
  }

  private static String requiredEnvironment(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing local integration-test configuration: " + name);
    }
    return value;
  }

  @Autowired private BusinessProfileService business;
  @Autowired private IdentityService identity;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MockMvc mockMvc;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AuditEventWriter audit;
  @Autowired private NormalizationService normalization;
  @Autowired private CurrentActor currentActor;
  @Autowired private CampaignService campaigns;
  @Autowired private ProspectApplicationService prospects;
  @Autowired private TagService tags;

  @Test
  void bootstrapIsIdempotentAndPreservesOperatorEditsAndPasswords() {
    UUID org = BusinessProfileProperties.SCALARIS_ORGANIZATION_ID;
    Integer prospectCount =
        jdbc.queryForObject(
            "SELECT count(*) FROM prospect WHERE organization_id = ?", Integer.class, org);
    String passwordHash =
        jdbc.queryForObject(
            "SELECT password_hash FROM app_user WHERE normalized_username = 'scalaris-marketing'",
            String.class);
    jdbc.update("UPDATE organization SET follow_up_days = 5 WHERE id = ?", org);
    jdbc.update(
        "UPDATE crm_tag SET color = '#0B1F33', active = FALSE WHERE organization_id = ? AND name = 'PC'",
        org);
    jdbc.update(
        "UPDATE crm_tag SET name = 'Notebook comercial', normalized_name = 'notebook comercial' WHERE organization_id = ? AND name = 'Notebook'",
        org);
    jdbc.update(
        "UPDATE template_version SET text_body = 'Texto editado por Marketing' WHERE organization_id = ? AND subject = 'Desarrollo para tu negocio'",
        org);
    business.initializeIfRequired();
    business.initializeIfRequired();
    business.bootstrapMarketingIfRequired();
    identity.bootstrapIfRequired(org);

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM organization WHERE slug = 'scalaris-local'", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT name FROM organization WHERE id = ?",
                String.class,
                TenantIds.BOOTSTRAP_ORGANIZATION_ID))
        .isEqualTo("Gestudio Local");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM crm_tag WHERE organization_id = ?", Integer.class, org))
        .isEqualTo(ScalarisCatalog.TAGS.size());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM template_version WHERE organization_id = ?",
                Integer.class,
                org))
        .isEqualTo(ScalarisCatalog.TEMPLATES.size());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM prospect WHERE organization_id = ?", Integer.class, org))
        .isEqualTo(prospectCount);
    assertThat(
            jdbc.queryForObject(
                "SELECT follow_up_days FROM organization WHERE id = ?", Integer.class, org))
        .isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "SELECT active FROM crm_tag WHERE organization_id = ? AND name = 'PC'",
                Boolean.class,
                org))
        .isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT text_body FROM template_version WHERE organization_id = ? AND subject = 'Desarrollo para tu negocio'",
                String.class,
                org))
        .isEqualTo("Texto editado por Marketing");
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM app_user WHERE normalized_username = 'scalaris-marketing'",
                String.class))
        .isEqualTo(passwordHash);
  }

  @Test
  void marketingCanOperateBusinessDataButCannotAdministerOrScheduleRealDelivery() throws Exception {
    CrmPrincipal marketing = (CrmPrincipal) identity.loadUserByUsername("scalaris-marketing");
    assertThat(marketing.organizationId())
        .isEqualTo(BusinessProfileProperties.SCALARIS_ORGANIZATION_ID);
    assertThat(marketing.permissions())
        .containsExactlyInAnyOrderElementsOf(ScalarisCatalog.MARKETING_PERMISSIONS);
    var login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"scalaris-marketing\",\"password\":\"synthetic-marketing-1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("MARKETING"))
            .andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
    mockMvc
        .perform(get("/api/v1/business-profile").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SCALARIS"))
        .andExpect(jsonPath("$.whatsappDisplay").value("11 7278-9289"))
        .andExpect(jsonPath("$.coverage").value("Zona Oeste, CABA y Mar del Plata"));
    mockMvc.perform(get("/api/v1/prospects").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/templates").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/tags").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/users").session(session)).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/v1/audit").session(session)).andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/v1/settings")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
          {"version":0,"name":"Scalaris","timezone":"America/Argentina/Buenos_Aires","currency":"ARS",
          "locale":"es-AR","brandingPrimaryColor":"#145CDB","followUpDays":3,
          "operatingWindowStart":"09:00","operatingWindowEnd":"18:00","businessDays":[1,2,3,4,5]}
          """))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/v1/campaigns/{id}/schedule", UUID.randomUUID())
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"version\":0,\"confirmation\":\"ENABLE_REAL_GMAIL_DELIVERY\",\"scheduledAt\":\"2030-01-01T12:00:00Z\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void existingLegacyAdminDoesNotPreventBootstrapOfTheScalarisAdmin() {
    identity.createUser(
        TenantIds.BOOTSTRAP_ORGANIZATION_ID,
        "legacy-owner",
        "Legacy",
        "synthetic-legacy-1",
        "ADMIN",
        null);
    UUID org = UUID.randomUUID();
    UUID role = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO organization (id, created_at, updated_at, slug, name) VALUES (?, now(), now(), ?, 'Bootstrap regression')",
        org,
        "bootstrap-" + org);
    jdbc.update(
        "INSERT INTO crm_role (id, organization_id, name, description) VALUES (?, ?, 'ADMIN', 'Synthetic bootstrap regression')",
        role,
        org);
    var scopedIdentity =
        new IdentityService(
            jdbc,
            passwordEncoder,
            new SecurityBootstrapProperties("scoped-owner", "synthetic-scoped-1", 12),
            audit);
    scopedIdentity.bootstrapIfRequired(org);
    var admin = (CrmPrincipal) identity.loadUserByUsername("scoped-owner");
    assertThat(admin.organizationId()).isEqualTo(org);
    assertThat(admin.role()).isEqualTo("ADMIN");
  }

  @Test
  void profileDoesNotLeakScalarisIdentityToHistoricalOrganizations() throws Exception {
    identity.createUser(
        TenantIds.BOOTSTRAP_ORGANIZATION_ID,
        "legacy-viewer",
        "Legacy viewer",
        "synthetic-viewer-1",
        "VIEWER",
        null);
    var login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"legacy-viewer\",\"password\":\"synthetic-viewer-1\"}"))
            .andExpect(status().isOk())
            .andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
    mockMvc
        .perform(get("/api/v1/business-profile").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("LEGACY"))
        .andExpect(jsonPath("$.whatsappUrl").value(""));
    mockMvc
        .perform(get("/api/v1/templates").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void bootstrapDoesNotRepurposeAnExistingAdministratorAsMarketing() {
    var changed =
        new BusinessProfileService(
            jdbc,
            new BusinessProfileProperties(
                "SCALARIS", "scalaris-admin", "synthetic-other-1", "Marketing"),
            normalization,
            identity,
            currentActor);
    org.assertj.core.api.Assertions.assertThatThrownBy(changed::bootstrapMarketingIfRequired)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already assigned");
    assertThat(((CrmPrincipal) identity.loadUserByUsername("scalaris-admin")).role())
        .isEqualTo("ADMIN");
  }

  @Test
  void campaignAudienceUsesOneActiveTenantTagAndRejectsForeignOrMissingTags() {
    CrmPrincipal marketing = (CrmPrincipal) identity.loadUserByUsername("scalaris-marketing");
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                marketing, marketing.password(), marketing.getAuthorities()));
    try {
      UUID technical =
          tags.list(false).stream()
              .filter(t -> t.name().equals("Servicio técnico"))
              .findFirst()
              .orElseThrow()
              .id();
      UUID development =
          tags.list(false).stream()
              .filter(t -> t.name().equals("Desarrollo"))
              .findFirst()
              .orElseThrow()
              .id();
      UUID technicalProspect = createSyntheticProspect("Technical campaign " + UUID.randomUUID());
      UUID developmentProspect =
          createSyntheticProspect("Development campaign " + UUID.randomUUID());
      tags.assign(technical, List.of(technicalProspect));
      tags.assign(development, List.of(developmentProspect));
      var template =
          campaigns.templates().stream()
              .filter(t -> t.channel() == CampaignChannel.EMAIL)
              .findFirst()
              .orElseThrow();
      var campaign =
          campaigns.createCampaign(
              new CreateCampaignCommand(
                  "Marketing tag regression",
                  "Synthetic",
                  "Segmentation",
                  CampaignChannel.EMAIL,
                  template.versionId()));
      var frozen =
          campaigns.freezeAudience(
              campaign.id(),
              campaign.version(),
              new AudienceFilter(null, null, null, null, null, null, true, false, technical));
      assertThat(frozen.recipientCount()).isEqualTo(1);
      assertThat(campaigns.audience(campaign.id()))
          .extracting(CampaignService.AudienceRecipientView::prospectId)
          .containsExactly(technicalProspect);

      UUID foreignTag = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO crm_tag (id, organization_id, name, normalized_name, color, created_at, updated_at) VALUES (?, ?, 'Foreign tag', 'foreign tag', '#145CDB', now(), now())",
          foreignTag,
          TenantIds.BOOTSTRAP_ORGANIZATION_ID);
      for (UUID unavailable : List.of(foreignTag, UUID.randomUUID())) {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () ->
                    campaigns.freezeAudience(
                        campaign.id(),
                        frozen.version(),
                        new AudienceFilter(
                            null, null, null, null, null, null, true, false, unavailable)))
            .isInstanceOf(ResourceNotFoundException.class);
      }
      jdbc.update("UPDATE crm_tag SET active = FALSE WHERE id = ?", technical);
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  campaigns.freezeAudience(
                      campaign.id(),
                      frozen.version(),
                      new AudienceFilter(
                          null, null, null, null, null, null, true, false, technical)))
          .isInstanceOf(ResourceNotFoundException.class);
      assertThat(campaigns.audience(campaign.id()))
          .extracting(CampaignService.AudienceRecipientView::prospectId)
          .containsExactly(technicalProspect);
      jdbc.update("UPDATE crm_tag SET active = TRUE WHERE id = ?", technical);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private UUID createSyntheticProspect(String name) {
    String suffix = UUID.randomUUID().toString();
    return prospects
        .create(
            new CreateProspectCommand(
                name,
                "Comercios",
                "Localidad sintética",
                "QA",
                "Argentina",
                null,
                "Contacto",
                "Sintético",
                suffix + "@example.test",
                null,
                null,
                "QA-" + suffix,
                "TEST",
                "Synthetic",
                null,
                2,
                50,
                null,
                "Consulta técnica",
                null,
                null))
        .id();
  }
}
