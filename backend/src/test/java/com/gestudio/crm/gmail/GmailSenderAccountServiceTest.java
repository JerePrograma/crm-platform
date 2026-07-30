package com.gestudio.crm.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.messaging.MessagingProperties;
import com.gestudio.crm.security.CurrentActor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class GmailSenderAccountServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

  private GmailSenderAccountRepository repository;
  private GmailOAuthStateService stateService;
  private GoogleOAuthClient oauthClient;
  private CurrentActor currentActor;
  private AuditEventWriter audit;
  private GmailDeliveryProperties properties;
  private GmailTokenCipher cipher;
  private GmailSenderAccountService service;

  @BeforeEach
  void setUp() {
    repository = mock(GmailSenderAccountRepository.class);
    stateService = mock(GmailOAuthStateService.class);
    oauthClient = mock(GoogleOAuthClient.class);
    currentActor = mock(CurrentActor.class);
    audit = mock(AuditEventWriter.class);
    properties = GmailTestProperties.properties("https://example.test");
    cipher = new GmailTokenCipher(properties);
    CrmPrincipal principal =
        new CrmPrincipal(
            USER_ID,
            ORGANIZATION_ID,
            "gmail-admin",
            "Gmail Admin",
            "not-used",
            "ADMIN",
            Set.of("SETTINGS_MANAGE", "CAMPAIGN_READ"),
            true,
            null);
    when(currentActor.requiredPrincipal()).thenReturn(principal);
    when(currentActor.organizationId()).thenReturn(ORGANIZATION_ID);
    service =
        new GmailSenderAccountService(
            repository,
            stateService,
            oauthClient,
            cipher,
            properties,
            new MessagingProperties(
                "GMAIL_LIVE",
                "DEEPLINK_ONLY",
                true,
                Duration.ofSeconds(2),
                new MessagingProperties.Gmail("https://gmail.googleapis.com", "", ""),
                new MessagingProperties.WhatsApp("", "", "", "", "", 4096)),
            currentActor,
            audit,
            new NormalizationService(),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void callbackStoresOnlyEncryptedRefreshTokenAndReturnsSafeLocation() {
    UUID accountId = UUID.randomUUID();
    when(stateService.consume("synthetic-state", ORGANIZATION_ID, USER_ID, "session-a"))
        .thenReturn(new GmailOAuthStateService.ConsumedState(null));
    when(oauthClient.exchangeAuthorizationCode("synthetic-code"))
        .thenReturn(
            new GoogleOAuthClient.OAuthTokens(
                "synthetic-access",
                "synthetic-refresh",
                NOW.plusSeconds(3600),
                Set.of("email", GmailDeliveryProperties.SEND_SCOPE)));
    when(oauthClient.userInfo("synthetic-access"))
        .thenReturn(
            new GoogleOAuthClient.UserInfo("Sender@Example.Test", "Synthetic Sender", true));
    when(repository.findByEmail(ORGANIZATION_ID, "sender@example.test")).thenReturn(null);
    when(repository.saveConnected(
            eq(ORGANIZATION_ID),
            eq(null),
            eq("sender@example.test"),
            eq("sender@example.test"),
            eq("Synthetic Sender"),
            eq(Set.of("email", GmailDeliveryProperties.SEND_SCOPE)),
            any(GmailTokenCipher.EncryptedSecret.class),
            eq(USER_ID),
            eq(NOW)))
        .thenAnswer(
            invocation ->
                account(
                    accountId,
                    "sender@example.test",
                    invocation.getArgument(6),
                    GmailSenderAccountStatus.CONNECTED));

    var location = service.callback("synthetic-state", "synthetic-code", null, "session-a");

    assertThat(location.toString())
        .isEqualTo("https://example.test/settings?gmail=connected")
        .doesNotContain(
            "synthetic-state", "synthetic-code", "synthetic-access", "synthetic-refresh");
    ArgumentCaptor<GmailTokenCipher.EncryptedSecret> encrypted =
        ArgumentCaptor.forClass(GmailTokenCipher.EncryptedSecret.class);
    verify(repository)
        .saveConnected(
            eq(ORGANIZATION_ID),
            eq(null),
            eq("sender@example.test"),
            eq("sender@example.test"),
            eq("Synthetic Sender"),
            eq(Set.of("email", GmailDeliveryProperties.SEND_SCOPE)),
            encrypted.capture(),
            eq(USER_ID),
            eq(NOW));
    assertThat(cipher.decrypt(encrypted.getValue())).isEqualTo("synthetic-refresh");
    assertThat(encrypted.getValue().toString()).doesNotContain("synthetic-refresh");
  }

  @Test
  void reconnectRetainsExistingRefreshTokenWhenGoogleOmitsANewOne() {
    UUID accountId = UUID.randomUUID();
    GmailSenderAccount existing =
        account(
            accountId,
            "sender@example.test",
            cipher.encrypt("existing-refresh"),
            GmailSenderAccountStatus.REAUTH_REQUIRED);
    when(stateService.consume("synthetic-state", ORGANIZATION_ID, USER_ID, "session-a"))
        .thenReturn(new GmailOAuthStateService.ConsumedState(accountId));
    when(oauthClient.exchangeAuthorizationCode("synthetic-code"))
        .thenReturn(
            new GoogleOAuthClient.OAuthTokens(
                "new-access",
                null,
                NOW.plusSeconds(3600),
                Set.of("email", GmailDeliveryProperties.SEND_SCOPE)));
    when(oauthClient.userInfo("new-access"))
        .thenReturn(
            new GoogleOAuthClient.UserInfo("sender@example.test", "Synthetic Sender", true));
    when(repository.find(ORGANIZATION_ID, accountId)).thenReturn(existing);
    when(repository.saveConnected(
            eq(ORGANIZATION_ID),
            eq(accountId),
            any(),
            any(),
            any(),
            any(),
            any(),
            eq(USER_ID),
            eq(NOW)))
        .thenAnswer(
            invocation ->
                account(
                    accountId,
                    "sender@example.test",
                    invocation.getArgument(6),
                    GmailSenderAccountStatus.CONNECTED));

    assertThat(service.callback("synthetic-state", "synthetic-code", null, "session-a").toString())
        .contains("gmail=connected");
    ArgumentCaptor<GmailTokenCipher.EncryptedSecret> encrypted =
        ArgumentCaptor.forClass(GmailTokenCipher.EncryptedSecret.class);
    verify(repository)
        .saveConnected(
            eq(ORGANIZATION_ID),
            eq(accountId),
            eq("sender@example.test"),
            eq("sender@example.test"),
            eq("Synthetic Sender"),
            eq(Set.of("email", GmailDeliveryProperties.SEND_SCOPE)),
            encrypted.capture(),
            eq(USER_ID),
            eq(NOW));
    assertThat(cipher.decrypt(encrypted.getValue())).isEqualTo("existing-refresh");
  }

  @Test
  void invalidGrantMarksReauthenticationAndAccessTokenCannotSerialize() throws Exception {
    UUID accountId = UUID.randomUUID();
    GmailSenderAccount account =
        account(
            accountId,
            "sender@example.test",
            cipher.encrypt("revoked-refresh"),
            GmailSenderAccountStatus.CONNECTED);
    when(repository.find(ORGANIZATION_ID, accountId)).thenReturn(account);
    when(oauthClient.refreshAccessToken("revoked-refresh"))
        .thenThrow(
            new GoogleOAuthException(
                GoogleOAuthException.Code.INVALID_GRANT, "Gmail authorization must be renewed"));

    assertThatThrownBy(() -> service.accessTokenFor(ORGANIZATION_ID, accountId))
        .isInstanceOfSatisfying(
            GmailProblemException.class,
            exception -> assertThat(exception.code()).isEqualTo("GMAIL_REAUTH_REQUIRED"));
    verify(repository).markReauthRequired(ORGANIZATION_ID, accountId, NOW);

    GmailAccessToken accessToken = new GmailAccessToken("synthetic-access", NOW.plusSeconds(60));
    assertThat(accessToken.toString()).doesNotContain("synthetic-access").contains("REDACTED");
    assertThat(new ObjectMapper().writeValueAsString(accessToken))
        .doesNotContain("synthetic-access");
  }

  private GmailSenderAccount account(
      UUID id,
      String email,
      GmailTokenCipher.EncryptedSecret secret,
      GmailSenderAccountStatus status) {
    return new GmailSenderAccount(
        id,
        0,
        ORGANIZATION_ID,
        email,
        email,
        "Synthetic Sender",
        status,
        true,
        Set.of("email", GmailDeliveryProperties.SEND_SCOPE),
        secret,
        USER_ID,
        NOW,
        NOW,
        null,
        null,
        10,
        60,
        null);
  }
}
