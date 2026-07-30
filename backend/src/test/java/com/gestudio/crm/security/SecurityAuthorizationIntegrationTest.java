package com.gestudio.crm.security;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gestudio.crm.common.TenantIds;
import com.gestudio.crm.identity.IdentityService;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    properties = {
      "security.bootstrap.username=test-owner",
      "security.bootstrap.password=test-password"
    })
@AutoConfigureMockMvc
@Testcontainers
class SecurityAuthorizationIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private IdentityService identityService;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void healthAndCsrfBootstrapArePublicButBusinessApiRejectsAnonymousRequests() throws Exception {
    mockMvc
        .perform(get("/actuator/health").header("X-Correlation-ID", "synthetic-correlation-123"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Correlation-ID", "synthetic-correlation-123"))
        .andExpect(header().string("X-Request-ID", "synthetic-correlation-123"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(
            header()
                .string(
                    "Content-Security-Policy",
                    "default-src 'self'; frame-ancestors 'none'; object-src 'none'"))
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    mockMvc
        .perform(get("/api/v1/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
    mockMvc.perform(get("/api/v1/prospects")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
  }

  @Test
  void loginCreatesAnEightHourSessionAndLogoutInvalidatesIt() throws Exception {
    MvcResult login =
        login("test-owner", "test-password")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("test-owner"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.permissions", hasItem("USER_MANAGE")))
            .andReturn();

    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
    org.assertj.core.api.Assertions.assertThat(session).isNotNull();
    org.assertj.core.api.Assertions.assertThat(session.getMaxInactiveInterval())
        .isEqualTo(8 * 60 * 60);

    mockMvc.perform(get("/api/v1/prospects").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/metrics").session(session)).andExpect(status().isOk());
    mockMvc
        .perform(post("/api/v1/auth/logout").session(session).with(csrf()))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/prospects").session(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void loginRequiresCsrfAndRejectsBadCredentials() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-owner\",\"password\":\"test-password\"}"))
        .andExpect(status().isForbidden());
    login("test-owner", "wrong-password").andExpect(status().isUnauthorized());
  }

  @Test
  void bootstrapPasswordIsOneWayHashedAndLoginIsAudited() throws Exception {
    String hash =
        jdbcTemplate.queryForObject(
            "SELECT password_hash FROM app_user WHERE normalized_username = 'test-owner'",
            String.class);
    org.assertj.core.api.Assertions.assertThat(hash).isNotEqualTo("test-password").startsWith("{");

    login("test-owner", "test-password").andExpect(status().isOk());
    Integer audits =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM audit_event WHERE action = 'LOGIN_SUCCEEDED'", Integer.class);
    org.assertj.core.api.Assertions.assertThat(audits).isPositive();
  }

  @Test
  void repeatedFailuresTemporarilyLockTheAccount() throws Exception {
    identityService.createUser(
        TenantIds.BOOTSTRAP_ORGANIZATION_ID,
        "locked-user",
        "Locked User",
        "initial-password-1",
        "SALES",
        null);

    for (int attempt = 0; attempt < 5; attempt++) {
      login("locked-user", "wrong-password").andExpect(status().isUnauthorized());
    }
    login("locked-user", "initial-password-1").andExpect(status().isUnauthorized());

    Integer locked =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM app_user WHERE normalized_username = 'locked-user' AND locked_until > now()",
            Integer.class);
    org.assertj.core.api.Assertions.assertThat(locked).isEqualTo(1);
  }

  @Test
  void viewerCanReadButCannotMutate() throws Exception {
    identityService.createUser(
        TenantIds.BOOTSTRAP_ORGANIZATION_ID,
        "read-only-user",
        "Read Only",
        "viewer-password-1",
        "VIEWER",
        null);
    MockHttpSession session = session(login("read-only-user", "viewer-password-1").andReturn());

    mockMvc.perform(get("/api/v1/prospects").session(session)).andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/prospects/metrics").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interested").isNumber())
        .andExpect(jsonPath("$.blocked").isNumber());
    mockMvc.perform(get("/api/v1/templates").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/campaigns").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/messages/safety").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/outbox").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/inbound").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/reports/dashboard").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/settings").session(session)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/sender-accounts").session(session)).andExpect(status().isOk());
    mockMvc
        .perform(post("/api/v1/sender-accounts/gmail/oauth/start").session(session).with(csrf()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/v1/campaigns/00000000-0000-0000-0000-000000000001/start")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"confirmation\":\"SEND_LIVE_CAMPAIGN\"}"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/v1/prospects")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"institutionName\":\"Viewer cannot create\"}"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/v1/settings")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "version":0,
                      "name":"Gestudio Local",
                      "timezone":"America/Argentina/Buenos_Aires",
                      "currency":"ARS",
                      "locale":"es-AR",
                      "brandingPrimaryColor":"#0f766e",
                      "followUpDays":3,
                      "operatingWindowStart":"09:00",
                      "operatingWindowEnd":"18:00",
                      "businessDays":[1,2,3,4,5]
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(post("/api/v1/outbox/worker/run-once").session(session).with(csrf()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/v1/messages/drafts")
                .session(session)
                .with(csrf())
                .header("Idempotency-Key", "viewer-cannot-draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "prospectId":"00000000-0000-0000-0000-000000000001",
                      "contactId":"00000000-0000-0000-0000-000000000002",
                      "channel":"EMAIL",
                      "subject":"Denied",
                      "textBody":"Denied"
                    }
                    """))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/v1/templates")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name":"Viewer cannot create template",
                      "channel":"EMAIL",
                      "subject":"Subject",
                      "textBody":"Text",
                      "htmlBody":"<p>Text</p>"
                    }
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void authenticatedAdminStillHasNoRealSendEndpoint() throws Exception {
    MockHttpSession session = session(login("test-owner", "test-password").andReturn());
    mockMvc
        .perform(
            post("/api/v1/messages/send")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void gmailMutationsRequireCsrfWhileOpaqueUnsubscribeIsPublicAndIdempotent() throws Exception {
    MockHttpSession session = session(login("test-owner", "test-password").andReturn());
    mockMvc
        .perform(post("/api/v1/sender-accounts/gmail/oauth/start").session(session))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/v1/unsubscribe/not-a-valid-token"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
    mockMvc
        .perform(post("/api/v1/unsubscribe/not-a-valid-token"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void fakeWebhookIsPublicAndCsrfExemptButStrictlyTypedAndDisabledByDefault() throws Exception {
    mockMvc.perform(get("/api/v1/webhooks/fake-inbound")).andExpect(status().isMethodNotAllowed());
    mockMvc
        .perform(
            post("/api/v1/webhooks/fake-inbound").contentType(MediaType.TEXT_PLAIN).content("{}"))
        .andExpect(status().isUnsupportedMediaType());
    mockMvc
        .perform(
            post("/api/v1/webhooks/fake-inbound")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Organization-Id", TenantIds.BOOTSTRAP_ORGANIZATION_ID)
                .header("X-Fake-Timestamp", 1)
                .header("X-Fake-Nonce", "disabled")
                .header("X-Fake-Signature", "00")
                .content("{}"))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void deactivatedUserCannotAuthenticate() throws Exception {
    var user =
        identityService.createUser(
            TenantIds.BOOTSTRAP_ORGANIZATION_ID,
            "inactive-user",
            "Inactive User",
            "inactive-password-1",
            "SALES",
            null);
    identityService.setActive(TenantIds.BOOTSTRAP_ORGANIZATION_ID, user.id(), false, null);

    login("inactive-user", "inactive-password-1").andExpect(status().isUnauthorized());
  }

  @Test
  void deactivationInvalidatesAnExistingSession() throws Exception {
    var user =
        identityService.createUser(
            TenantIds.BOOTSTRAP_ORGANIZATION_ID,
            "active-session-user",
            "Active Session",
            "active-password-1",
            "SALES",
            null);
    MockHttpSession session =
        session(login("active-session-user", "active-password-1").andReturn());
    identityService.setActive(TenantIds.BOOTSTRAP_ORGANIZATION_ID, user.id(), false, null);

    mockMvc.perform(get("/api/v1/prospects").session(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void passwordChangeInvalidatesTheSessionAndOldCredential() throws Exception {
    identityService.createUser(
        TenantIds.BOOTSTRAP_ORGANIZATION_ID,
        "password-user",
        "Password User",
        "original-password-1",
        "SALES",
        null);
    MockHttpSession session = session(login("password-user", "original-password-1").andReturn());

    mockMvc
        .perform(
            post("/api/v1/auth/password")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"currentPassword\":\"original-password-1\",\"newPassword\":\"replacement-password-2\"}"))
        .andExpect(status().isOk());
    login("password-user", "original-password-1").andExpect(status().isUnauthorized());
    login("password-user", "replacement-password-2").andExpect(status().isOk());
  }

  @Test
  void prospectReadsAreIsolatedByOrganization() throws Exception {
    MockHttpSession admin = session(login("test-owner", "test-password").andReturn());
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/prospects")
                    .session(admin)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"institutionName\":\"Tenant A School\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String prospectId =
        objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

    UUID organizationId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO organization (id, created_at, updated_at, slug, name)
        VALUES (?, now(), now(), ?, 'Tenant B')
        """,
        organizationId,
        "tenant-b-" + organizationId);
    jdbcTemplate.update(
        """
        INSERT INTO crm_role (id, organization_id, name, description)
        SELECT ?, ?, name, description
        FROM crm_role
        WHERE organization_id = ? AND name = 'VIEWER'
        """,
        roleId,
        organizationId,
        TenantIds.BOOTSTRAP_ORGANIZATION_ID);
    jdbcTemplate.update(
        """
        INSERT INTO role_permission (role_id, permission_code)
        SELECT ?, permission_code
        FROM role_permission
        WHERE role_id = '00000000-0000-0000-0000-000000000104'
        """,
        roleId);
    identityService.createUser(
        organizationId, "tenant-b-user", "Tenant B", "tenant-password-1", "VIEWER", null);
    MockHttpSession tenantB = session(login("tenant-b-user", "tenant-password-1").andReturn());
    mockMvc
        .perform(get("/api/v1/prospects/{id}", prospectId).session(tenantB))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/prospects/metrics").session(tenantB))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interested").value(0))
        .andExpect(jsonPath("$.blocked").value(0));
  }

  private org.springframework.test.web.servlet.ResultActions login(String username, String password)
      throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/login")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
  }

  private MockHttpSession session(MvcResult result) {
    HttpSession session = result.getRequest().getSession(false);
    org.assertj.core.api.Assertions.assertThat(session).isInstanceOf(MockHttpSession.class);
    return (MockHttpSession) session;
  }
}
