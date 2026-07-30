package com.gestudio.crm.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.identity.IdentityService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "security.bootstrap.username=gmail-admin",
      "security.bootstrap.password=gmail-password-1"
    })
@Testcontainers
class GmailPersistenceIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private GmailSenderAccountRepository repository;
  @Autowired private GmailDeliveryProperties applicationProperties;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private IdentityService identityService;

  @Test
  void keepsEncryptedTenantScopedAccountsAndOneDefault() {
    CrmPrincipal principal = (CrmPrincipal) identityService.loadUserByUsername("gmail-admin");
    GmailTokenCipher cipher =
        new GmailTokenCipher(GmailTestProperties.properties("https://example.test"));
    String refreshToken = "synthetic-refresh-token-that-is-not-plaintext-in-db";

    GmailSenderAccount first =
        repository.saveConnected(
            principal.organizationId(),
            null,
            "first@example.test",
            "first@example.test",
            "First Synthetic",
            Set.of(GmailDeliveryProperties.SEND_SCOPE, "email"),
            cipher.encrypt(refreshToken),
            principal.userId(),
            NOW);
    GmailSenderAccount second =
        repository.saveConnected(
            principal.organizationId(),
            null,
            "second@example.test",
            "second@example.test",
            "Second Synthetic",
            Set.of(GmailDeliveryProperties.SEND_SCOPE, "email"),
            cipher.encrypt(refreshToken),
            principal.userId(),
            NOW);

    assertThat(first.defaultAccount()).isTrue();
    assertThat(second.defaultAccount()).isFalse();
    assertThat(repository.find(UUID.randomUUID(), first.id())).isNull();
    assertThat(repository.list(principal.organizationId())).hasSize(2);
    byte[] stored =
        jdbcTemplate.queryForObject(
            "SELECT encrypted_credential FROM integration_connection WHERE id = ?",
            byte[].class,
            first.id());
    assertThat(stored).isNotNull();
    assertThat(new String(stored, java.nio.charset.StandardCharsets.UTF_8))
        .doesNotContain(refreshToken);
    assertThat(
            cipher.decrypt(
                repository.find(principal.organizationId(), first.id()).encryptedRefreshToken()))
        .isEqualTo(refreshToken);

    GmailSenderAccount newDefault =
        repository.setDefault(principal.organizationId(), second.id(), NOW.plusSeconds(1));
    assertThat(newDefault.defaultAccount()).isTrue();
    assertThat(repository.find(principal.organizationId(), first.id()).defaultAccount()).isFalse();

    GmailSenderAccount revoked =
        repository.revoke(principal.organizationId(), second.id(), NOW.plusSeconds(2));
    assertThat(revoked.status()).isEqualTo(GmailSenderAccountStatus.REVOKED);
    assertThat(revoked.encryptedRefreshToken()).isNull();
    assertThat(revoked.defaultAccount()).isFalse();
  }

  @Test
  void oauthStateIsSessionBoundSingleUseAndExpires() {
    CrmPrincipal principal = (CrmPrincipal) identityService.loadUserByUsername("gmail-admin");
    Clock issuedClock = Clock.fixed(NOW, ZoneOffset.UTC);
    GmailOAuthStateService issuer =
        new GmailOAuthStateService(
            jdbcTemplate, applicationProperties, issuedClock, new java.security.SecureRandom());

    var state =
        issuer.issue(principal.organizationId(), principal.userId(), "synthetic-session-a", null);
    assertThat(state.toString()).doesNotContain(state.value()).contains("REDACTED");
    assertThat(
            issuer
                .consume(
                    state.value(),
                    principal.organizationId(),
                    principal.userId(),
                    "synthetic-session-a")
                .reconnectAccountId())
        .isNull();
    assertProblem(
        () ->
            issuer.consume(
                state.value(),
                principal.organizationId(),
                principal.userId(),
                "synthetic-session-a"),
        "GMAIL_OAUTH_STATE_REPLAYED");

    var wrongSession =
        issuer.issue(principal.organizationId(), principal.userId(), "synthetic-session-a", null);
    assertProblem(
        () ->
            issuer.consume(
                wrongSession.value(),
                principal.organizationId(),
                principal.userId(),
                "synthetic-session-b"),
        "GMAIL_OAUTH_STATE_INVALID");

    var expiring =
        issuer.issue(principal.organizationId(), principal.userId(), "synthetic-session-a", null);
    GmailOAuthStateService expiredReader =
        new GmailOAuthStateService(
            jdbcTemplate,
            applicationProperties,
            Clock.fixed(NOW.plus(applicationProperties.stateTtl()).plusSeconds(1), ZoneOffset.UTC),
            new java.security.SecureRandom());
    assertProblem(
        () ->
            expiredReader.consume(
                expiring.value(),
                principal.organizationId(),
                principal.userId(),
                "synthetic-session-a"),
        "GMAIL_OAUTH_STATE_EXPIRED");
  }

  private void assertProblem(Runnable action, String code) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            GmailProblemException.class, exception -> assertThat(exception.code()).isEqualTo(code));
  }
}
