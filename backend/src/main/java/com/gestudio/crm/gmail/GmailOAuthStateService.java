package com.gestudio.crm.gmail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GmailOAuthStateService {

  private final JdbcTemplate jdbcTemplate;
  private final GmailDeliveryProperties properties;
  private final Clock clock;
  private final SecureRandom secureRandom;

  @Autowired
  public GmailOAuthStateService(
      JdbcTemplate jdbcTemplate, GmailDeliveryProperties properties, Clock clock) {
    this(jdbcTemplate, properties, clock, new SecureRandom());
  }

  GmailOAuthStateService(
      JdbcTemplate jdbcTemplate,
      GmailDeliveryProperties properties,
      Clock clock,
      SecureRandom secureRandom) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
    this.clock = clock;
    this.secureRandom = secureRandom;
  }

  @Transactional
  public IssuedState issue(
      UUID organizationId, UUID userId, String sessionId, UUID reconnectAccountId) {
    required(organizationId, userId, sessionId);
    byte[] random = new byte[32];
    secureRandom.nextBytes(random);
    String state = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    Instant now = clock.instant();
    Instant expiresAt = now.plus(properties.stateTtl());
    jdbcTemplate.update(
        """
        INSERT INTO gmail_oauth_state (
          id, state_hash, organization_id, user_id, session_hash,
          reconnect_account_id, expires_at, consumed_at, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?)
        """,
        UUID.randomUUID(),
        hash(state),
        organizationId,
        userId,
        hash(sessionId),
        reconnectAccountId,
        Timestamp.from(expiresAt),
        Timestamp.from(now));
    return new IssuedState(state, expiresAt);
  }

  @Transactional
  public ConsumedState consume(String state, UUID organizationId, UUID userId, String sessionId) {
    required(organizationId, userId, sessionId);
    if (state == null || !state.matches("[A-Za-z0-9_-]{43}")) {
      throw problem("GMAIL_OAUTH_STATE_INVALID", "OAuth state is invalid");
    }
    String stateHash = hash(state);
    Instant now = clock.instant();
    List<ConsumedState> consumed =
        jdbcTemplate.query(
            """
            UPDATE gmail_oauth_state
               SET consumed_at = ?
             WHERE state_hash = ?
               AND organization_id = ?
               AND user_id = ?
               AND session_hash = ?
               AND consumed_at IS NULL
               AND expires_at > ?
            RETURNING reconnect_account_id
            """,
            (resultSet, rowNumber) ->
                new ConsumedState((UUID) resultSet.getObject("reconnect_account_id")),
            Timestamp.from(now),
            stateHash,
            organizationId,
            userId,
            hash(sessionId),
            Timestamp.from(now));
    if (!consumed.isEmpty()) {
      return consumed.getFirst();
    }
    StateFailure failure =
        jdbcTemplate.query(
            "SELECT expires_at, consumed_at FROM gmail_oauth_state WHERE state_hash = ?",
            resultSet -> {
              if (!resultSet.next()) {
                return StateFailure.INVALID;
              }
              Timestamp consumedAt = resultSet.getTimestamp("consumed_at");
              if (consumedAt != null) {
                return StateFailure.REPLAYED;
              }
              Timestamp expiresAt = resultSet.getTimestamp("expires_at");
              return expiresAt != null && !expiresAt.toInstant().isAfter(now)
                  ? StateFailure.EXPIRED
                  : StateFailure.INVALID;
            },
            stateHash);
    throw switch (failure) {
      case EXPIRED -> problem("GMAIL_OAUTH_STATE_EXPIRED", "OAuth state has expired");
      case REPLAYED -> problem("GMAIL_OAUTH_STATE_REPLAYED", "OAuth state has already been used");
      case INVALID -> problem("GMAIL_OAUTH_STATE_INVALID", "OAuth state is invalid");
    };
  }

  private GmailProblemException problem(String code, String detail) {
    return new GmailProblemException(HttpStatus.BAD_REQUEST, code, detail);
  }

  private void required(UUID organizationId, UUID userId, String sessionId) {
    if (organizationId == null || userId == null || sessionId == null || sessionId.isBlank()) {
      throw new IllegalArgumentException("OAuth actor and session context are required");
    }
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private enum StateFailure {
    INVALID,
    EXPIRED,
    REPLAYED
  }

  public record IssuedState(@JsonIgnore String value, Instant expiresAt) {
    @Override
    public String toString() {
      return "IssuedState[REDACTED,expiresAt=" + expiresAt + "]";
    }
  }

  public record ConsumedState(UUID reconnectAccountId) {}
}
