package com.gestudio.crm.gmail;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.identity.CrmPrincipal;
import com.gestudio.crm.messaging.MessagingProperties;
import com.gestudio.crm.security.CurrentActor;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GmailSenderAccountService {

  private final GmailSenderAccountRepository repository;
  private final GmailOAuthStateService stateService;
  private final GoogleOAuthClient oauthClient;
  private final GmailTokenCipher tokenCipher;
  private final GmailDeliveryProperties properties;
  private final MessagingProperties messagingProperties;
  private final CurrentActor currentActor;
  private final AuditEventWriter audit;
  private final NormalizationService normalization;
  private final Clock clock;

  public GmailSenderAccountService(
      GmailSenderAccountRepository repository,
      GmailOAuthStateService stateService,
      GoogleOAuthClient oauthClient,
      GmailTokenCipher tokenCipher,
      GmailDeliveryProperties properties,
      MessagingProperties messagingProperties,
      CurrentActor currentActor,
      AuditEventWriter audit,
      NormalizationService normalization,
      Clock clock) {
    this.repository = repository;
    this.stateService = stateService;
    this.oauthClient = oauthClient;
    this.tokenCipher = tokenCipher;
    this.properties = properties;
    this.messagingProperties = messagingProperties;
    this.currentActor = currentActor;
    this.audit = audit;
    this.normalization = normalization;
    this.clock = clock;
  }

  public List<SenderAccountView> list() {
    return repository.list(currentActor.organizationId()).stream().map(this::view).toList();
  }

  public ConfigurationView configuration() {
    return new ConfigurationView(
        properties.oauthConfigured(),
        messagingProperties.emailMode(),
        messagingProperties.realNetworkAllowed(),
        GmailDeliveryProperties.SEND_SCOPE);
  }

  public OAuthStartView start(String sessionId) {
    properties.requireLiveConfigured();
    CrmPrincipal principal = currentActor.requiredPrincipal();
    GmailOAuthStateService.IssuedState state =
        stateService.issue(principal.organizationId(), principal.userId(), sessionId, null);
    URI authorization = oauthClient.authorizationUri(state.value());
    audit.record(
        "GMAIL_OAUTH_STARTED",
        "GMAIL_SENDER_ACCOUNT",
        null,
        Map.of("reconnect", false, "expiresAt", state.expiresAt().toString()));
    return new OAuthStartView(authorization, state.expiresAt());
  }

  public OAuthStartView reconnect(UUID accountId, String sessionId) {
    properties.requireLiveConfigured();
    CrmPrincipal principal = currentActor.requiredPrincipal();
    GmailSenderAccount account = required(principal.organizationId(), accountId);
    GmailOAuthStateService.IssuedState state =
        stateService.issue(principal.organizationId(), principal.userId(), sessionId, account.id());
    URI authorization = oauthClient.authorizationUri(state.value());
    audit.record(
        "GMAIL_OAUTH_STARTED",
        "GMAIL_SENDER_ACCOUNT",
        account.id(),
        Map.of("reconnect", true, "expiresAt", state.expiresAt().toString()));
    return new OAuthStartView(authorization, state.expiresAt());
  }

  public URI callback(String state, String code, String error, String sessionId) {
    CrmPrincipal principal = currentActor.requiredPrincipal();
    GmailOAuthStateService.ConsumedState consumed;
    try {
      consumed =
          stateService.consume(state, principal.organizationId(), principal.userId(), sessionId);
    } catch (RuntimeException exception) {
      audit.record(
          "GMAIL_OAUTH_CALLBACK_FAILED",
          "GMAIL_SENDER_ACCOUNT",
          null,
          Map.of("reason", safeCallbackReason(exception)));
      return callbackLocation("error", safeCallbackReason(exception));
    }
    UUID reconnectAccountId = consumed.reconnectAccountId();
    if (error != null && !error.isBlank()) {
      audit.record(
          "GMAIL_OAUTH_CALLBACK_FAILED",
          "GMAIL_SENDER_ACCOUNT",
          reconnectAccountId,
          Map.of("reason", "AUTHORIZATION_DENIED"));
      return callbackLocation("error", "AUTHORIZATION_DENIED");
    }
    if (code == null || code.isBlank()) {
      audit.record(
          "GMAIL_OAUTH_CALLBACK_FAILED",
          "GMAIL_SENDER_ACCOUNT",
          reconnectAccountId,
          Map.of("reason", "CALLBACK_CODE_MISSING"));
      return callbackLocation("error", "CALLBACK_CODE_MISSING");
    }
    try {
      GoogleOAuthClient.OAuthTokens tokens = oauthClient.exchangeAuthorizationCode(code);
      requireSendScope(tokens.scopes());
      GoogleOAuthClient.UserInfo info = oauthClient.userInfo(tokens.accessToken());
      String normalizedEmail = normalization.normalizeEmail(info.email());
      GmailSenderAccount existing =
          reconnectAccountId == null
              ? repository.findByEmail(principal.organizationId(), normalizedEmail)
              : required(principal.organizationId(), reconnectAccountId);
      if (reconnectAccountId != null && !existing.normalizedEmail().equals(normalizedEmail)) {
        throw problem(
            HttpStatus.CONFLICT,
            "GMAIL_RECONNECT_ACCOUNT_MISMATCH",
            "Reconnect must authorize the same Google account");
      }
      String refreshToken = tokens.refreshToken();
      if (refreshToken == null || refreshToken.isBlank()) {
        if (existing == null || existing.encryptedRefreshToken() == null) {
          throw problem(
              HttpStatus.BAD_GATEWAY,
              "GMAIL_REFRESH_TOKEN_MISSING",
              "Google did not issue offline authorization");
        }
        refreshToken = tokenCipher.decrypt(existing.encryptedRefreshToken());
      }
      GmailSenderAccount connected =
          repository.saveConnected(
              principal.organizationId(),
              existing == null ? null : existing.id(),
              normalizedEmail,
              normalizedEmail,
              displayName(info.displayName()),
              tokens.scopes(),
              tokenCipher.encrypt(refreshToken),
              principal.userId(),
              clock.instant());
      audit.record(
          "GMAIL_OAUTH_CALLBACK_SUCCEEDED",
          "GMAIL_SENDER_ACCOUNT",
          connected.id(),
          Map.of("account", mask(connected.emailAddress()), "scopes", connected.grantedScopes()));
      audit.record(
          "GMAIL_SENDER_ACCOUNT_CONNECTED",
          "GMAIL_SENDER_ACCOUNT",
          connected.id(),
          Map.of("account", mask(connected.emailAddress())));
      return callbackLocation("connected", null);
    } catch (RuntimeException exception) {
      if (reconnectAccountId != null
          && exception instanceof GoogleOAuthException oauth
          && oauth.code() == GoogleOAuthException.Code.INVALID_GRANT) {
        repository.markReauthRequired(
            principal.organizationId(), reconnectAccountId, clock.instant());
      }
      String reason = safeCallbackReason(exception);
      audit.record(
          "GMAIL_OAUTH_CALLBACK_FAILED",
          "GMAIL_SENDER_ACCOUNT",
          reconnectAccountId,
          Map.of("reason", reason));
      return callbackLocation("error", reason);
    }
  }

  public SenderAccountView verify(UUID accountId) {
    UUID organizationId = currentActor.organizationId();
    GmailSenderAccount account = required(organizationId, accountId);
    GoogleOAuthClient.OAuthTokens tokens = refresh(account);
    requireSendScope(account.grantedScopes());
    GoogleOAuthClient.UserInfo userInfo = oauthClient.userInfo(tokens.accessToken());
    if (!account.normalizedEmail().equals(normalization.normalizeEmail(userInfo.email()))) {
      repository.markError(
          organizationId, accountId, "Google account identity changed", clock.instant());
      throw problem(
          HttpStatus.CONFLICT,
          "GMAIL_ACCOUNT_IDENTITY_CHANGED",
          "Google account identity does not match the sender account");
    }
    GmailSenderAccount verified = repository.verify(organizationId, accountId, clock.instant());
    if (verified == null) {
      throw problem(
          HttpStatus.CONFLICT,
          "GMAIL_ACCOUNT_NOT_CONNECTED",
          "Gmail sender account is not connected");
    }
    audit.record(
        "GMAIL_SENDER_ACCOUNT_VERIFIED",
        "GMAIL_SENDER_ACCOUNT",
        accountId,
        Map.of("account", mask(verified.emailAddress())));
    return view(verified);
  }

  public SenderAccountView setDefault(UUID accountId) {
    GmailSenderAccount account =
        repository.setDefault(currentActor.organizationId(), accountId, clock.instant());
    if (account == null) {
      throw problem(
          HttpStatus.CONFLICT,
          "GMAIL_ACCOUNT_NOT_CONNECTED",
          "Only a connected Gmail account can be the default sender");
    }
    audit.record(
        "GMAIL_SENDER_ACCOUNT_DEFAULTED",
        "GMAIL_SENDER_ACCOUNT",
        accountId,
        Map.of("account", mask(account.emailAddress())));
    return view(account);
  }

  public SenderAccountView revoke(UUID accountId) {
    UUID organizationId = currentActor.organizationId();
    GmailSenderAccount account = required(organizationId, accountId);
    String refreshToken =
        account.encryptedRefreshToken() == null
            ? null
            : tokenCipher.decrypt(account.encryptedRefreshToken());
    GmailSenderAccount revoked = repository.revoke(organizationId, accountId, clock.instant());
    if (revoked == null) {
      throw new ResourceNotFoundException("Gmail sender account not found: " + accountId);
    }
    audit.record(
        "GMAIL_SENDER_ACCOUNT_REVOKED",
        "GMAIL_SENDER_ACCOUNT",
        accountId,
        Map.of("account", mask(account.emailAddress())));
    if (refreshToken != null) {
      try {
        oauthClient.revoke(refreshToken);
      } catch (GoogleOAuthException ignored) {
        // Local revocation is authoritative and must not be rolled back by a remote failure.
      }
    }
    return view(revoked);
  }

  public GmailAccessToken accessTokenFor(UUID organizationId, UUID senderAccountId) {
    requireLiveMode();
    GmailSenderAccount account = required(organizationId, senderAccountId);
    if (account.status() != GmailSenderAccountStatus.CONNECTED
        || account.encryptedRefreshToken() == null) {
      throw problem(
          HttpStatus.CONFLICT,
          "GMAIL_ACCOUNT_NOT_CONNECTED",
          "Gmail sender account is not connected");
    }
    requireSendScope(account.grantedScopes());
    GoogleOAuthClient.OAuthTokens tokens = refresh(account);
    return new GmailAccessToken(tokens.accessToken(), tokens.expiresAt());
  }

  private GoogleOAuthClient.OAuthTokens refresh(GmailSenderAccount account) {
    if (account.status() == GmailSenderAccountStatus.REVOKED
        || account.encryptedRefreshToken() == null) {
      throw problem(
          HttpStatus.CONFLICT, "GMAIL_ACCOUNT_REVOKED", "Gmail sender account is revoked");
    }
    String refreshToken = tokenCipher.decrypt(account.encryptedRefreshToken());
    try {
      return oauthClient.refreshAccessToken(refreshToken);
    } catch (GoogleOAuthException exception) {
      if (exception.code() == GoogleOAuthException.Code.INVALID_GRANT) {
        repository.markReauthRequired(account.organizationId(), account.id(), clock.instant());
        audit.recordFor(
            account.organizationId(),
            null,
            "GMAIL_REAUTH_REQUIRED",
            "GMAIL_SENDER_ACCOUNT",
            account.id(),
            "FAILURE",
            Map.of("reason", "INVALID_GRANT"));
        throw problem(
            HttpStatus.CONFLICT,
            "GMAIL_REAUTH_REQUIRED",
            "Gmail sender account must be reconnected");
      }
      throw exception;
    }
  }

  private void requireLiveMode() {
    if (!"GMAIL_LIVE".equalsIgnoreCase(messagingProperties.emailMode())
        || !messagingProperties.realNetworkAllowed()) {
      throw problem(
          HttpStatus.CONFLICT, "GMAIL_LIVE_DISABLED", "Gmail live network delivery is disabled");
    }
    properties.requireLiveConfigured();
  }

  private void requireSendScope(Set<String> scopes) {
    if (scopes == null || !scopes.contains(GmailDeliveryProperties.SEND_SCOPE)) {
      throw problem(
          HttpStatus.CONFLICT,
          "GMAIL_SCOPE_MISSING",
          "Google did not grant the Gmail send permission");
    }
  }

  private GmailSenderAccount required(UUID organizationId, UUID accountId) {
    GmailSenderAccount account = repository.find(organizationId, accountId);
    if (account == null) {
      throw new ResourceNotFoundException("Gmail sender account not found: " + accountId);
    }
    return account;
  }

  private SenderAccountView view(GmailSenderAccount account) {
    return new SenderAccountView(
        account.id(),
        account.version(),
        "GMAIL",
        account.emailAddress(),
        account.displayName(),
        account.status(),
        account.defaultAccount(),
        account.grantedScopes(),
        account.connectedAt(),
        account.verifiedAt(),
        account.revokedAt(),
        account.lastErrorSummary(),
        account.dailyLimit(),
        account.minIntervalSeconds(),
        account.nextSendAt());
  }

  private URI callbackLocation(String outcome, String code) {
    String separator = properties.frontendReturn().toString().contains("?") ? "&" : "?";
    String suffix = "gmail=" + outcome + (code == null ? "" : "&code=" + code);
    return URI.create(properties.frontendReturn() + separator + suffix);
  }

  private String safeCallbackReason(Throwable exception) {
    if (exception instanceof GmailProblemException gmail) {
      return gmail.code();
    }
    if (exception instanceof GoogleOAuthException oauth) {
      return "GOOGLE_" + oauth.code().name();
    }
    if (exception instanceof IllegalArgumentException) {
      return "INVALID_CALLBACK_RESPONSE";
    }
    return "OAUTH_CALLBACK_FAILED";
  }

  private String displayName(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() > 200 || trimmed.indexOf('\r') >= 0 || trimmed.indexOf('\n') >= 0) {
      return null;
    }
    return trimmed;
  }

  private String mask(String email) {
    if (email == null || !email.contains("@")) {
      return "hidden";
    }
    String[] parts = email.toLowerCase(Locale.ROOT).split("@", 2);
    return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
  }

  private GmailProblemException problem(HttpStatus status, String code, String detail) {
    return new GmailProblemException(status, code, detail);
  }

  public record OAuthStartView(URI authorizationUrl, Instant expiresAt) {}

  public record ConfigurationView(
      boolean oauthConfigured,
      String providerMode,
      boolean realNetworkAllowed,
      String requiredScope) {}
}
