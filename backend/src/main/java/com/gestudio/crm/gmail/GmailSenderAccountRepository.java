package com.gestudio.crm.gmail;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class GmailSenderAccountRepository {

  private static final String SELECT_COLUMNS =
      """
      SELECT id, version, organization_id, email_address, normalized_email, display_name,
        status, is_default, granted_scopes, encrypted_credential, credential_nonce,
        credential_key_id, connected_by, connected_at, verified_at, revoked_at,
        last_error_summary, daily_limit, min_interval_seconds, next_send_at
      FROM integration_connection
      """;

  private final JdbcTemplate jdbcTemplate;

  GmailSenderAccountRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional(readOnly = true)
  List<GmailSenderAccount> list(UUID organizationId) {
    return jdbcTemplate.query(
        SELECT_COLUMNS
            + " WHERE organization_id = ? AND provider = 'GMAIL' AND normalized_email IS NOT NULL"
            + " ORDER BY is_default DESC, email_address",
        this::map,
        organizationId);
  }

  @Transactional(readOnly = true)
  GmailSenderAccount find(UUID organizationId, UUID accountId) {
    return jdbcTemplate
        .query(
            SELECT_COLUMNS
                + " WHERE organization_id = ? AND id = ? AND provider = 'GMAIL'"
                + " AND normalized_email IS NOT NULL",
            this::map,
            organizationId,
            accountId)
        .stream()
        .findFirst()
        .orElse(null);
  }

  @Transactional(readOnly = true)
  GmailSenderAccount findByEmail(UUID organizationId, String normalizedEmail) {
    return jdbcTemplate
        .query(
            SELECT_COLUMNS
                + " WHERE organization_id = ? AND provider = 'GMAIL' AND normalized_email = ?",
            this::map,
            organizationId,
            normalizedEmail)
        .stream()
        .findFirst()
        .orElse(null);
  }

  @Transactional
  GmailSenderAccount saveConnected(
      UUID organizationId,
      UUID accountId,
      String emailAddress,
      String normalizedEmail,
      String displayName,
      Set<String> grantedScopes,
      GmailTokenCipher.EncryptedSecret secret,
      UUID connectedBy,
      Instant now) {
    lockOrganization(organizationId);
    GmailSenderAccount existing =
        accountId == null
            ? findByEmail(organizationId, normalizedEmail)
            : find(organizationId, accountId);
    UUID id = existing == null ? UUID.randomUUID() : existing.id();
    if (existing == null) {
      jdbcTemplate.update(
          """
          INSERT INTO integration_connection (
            id, version, organization_id, provider, mode, status, configuration,
            encrypted_credential, credential_key_id, credential_nonce, email_address,
            normalized_email, display_name, is_default, granted_scopes, connected_by,
            connected_at, verified_at, revoked_at, disconnected_at, last_checked_at,
            last_error_at, last_error_summary, daily_limit, min_interval_seconds,
            created_at, updated_at
          ) VALUES (
            ?, 0, ?, 'GMAIL', 'OAUTH', 'CONNECTED', '{}'::jsonb,
            ?, ?, ?, ?, ?, ?,
            NOT EXISTS (SELECT 1 FROM integration_connection
              WHERE organization_id = ? AND provider = 'GMAIL' AND is_default),
            string_to_array(?, ','), ?, ?, ?, NULL, NULL, ?, NULL, NULL, 10, 60, ?, ?
          )
          """,
          id,
          organizationId,
          secret.ciphertext(),
          secret.keyId(),
          secret.nonce(),
          emailAddress,
          normalizedEmail,
          displayName,
          organizationId,
          scopes(grantedScopes),
          connectedBy,
          Timestamp.from(now),
          Timestamp.from(now),
          Timestamp.from(now),
          Timestamp.from(now),
          Timestamp.from(now));
    } else {
      jdbcTemplate.update(
          """
          UPDATE integration_connection
             SET version = version + 1, mode = 'OAUTH', status = 'CONNECTED',
                 encrypted_credential = ?, credential_key_id = ?, credential_nonce = ?,
                 email_address = ?, normalized_email = ?, display_name = ?,
                 granted_scopes = string_to_array(?, ','), connected_by = ?,
                 connected_at = COALESCE(connected_at, ?), verified_at = ?, revoked_at = NULL,
                 disconnected_at = NULL, last_checked_at = ?, last_error_at = NULL,
                 last_error_summary = NULL, updated_at = ?
           WHERE organization_id = ? AND id = ? AND provider = 'GMAIL'
          """,
          secret.ciphertext(),
          secret.keyId(),
          secret.nonce(),
          emailAddress,
          normalizedEmail,
          displayName,
          scopes(grantedScopes),
          connectedBy,
          Timestamp.from(now),
          Timestamp.from(now),
          Timestamp.from(now),
          Timestamp.from(now),
          organizationId,
          id);
    }
    return find(organizationId, id);
  }

  @Transactional
  GmailSenderAccount verify(UUID organizationId, UUID accountId, Instant now) {
    int changed =
        jdbcTemplate.update(
            """
            UPDATE integration_connection
               SET version = version + 1, status = 'CONNECTED', verified_at = ?,
                   last_checked_at = ?, last_error_at = NULL, last_error_summary = NULL,
                   updated_at = ?
             WHERE organization_id = ? AND id = ? AND provider = 'GMAIL'
               AND encrypted_credential IS NOT NULL AND status <> 'REVOKED'
            """,
            Timestamp.from(now),
            Timestamp.from(now),
            Timestamp.from(now),
            organizationId,
            accountId);
    return changed == 1 ? find(organizationId, accountId) : null;
  }

  @Transactional
  GmailSenderAccount setDefault(UUID organizationId, UUID accountId, Instant now) {
    lockOrganization(organizationId);
    Integer connected =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM integration_connection
            WHERE organization_id = ? AND id = ? AND provider = 'GMAIL'
              AND normalized_email IS NOT NULL AND status = 'CONNECTED'
            """,
            Integer.class,
            organizationId,
            accountId);
    if (connected == null || connected != 1) {
      return null;
    }
    jdbcTemplate.update(
        """
        UPDATE integration_connection SET is_default = FALSE, version = version + 1, updated_at = ?
        WHERE organization_id = ? AND provider = 'GMAIL' AND is_default
        """,
        Timestamp.from(now),
        organizationId);
    jdbcTemplate.update(
        """
        UPDATE integration_connection SET is_default = TRUE, version = version + 1, updated_at = ?
        WHERE organization_id = ? AND id = ? AND provider = 'GMAIL'
        """,
        Timestamp.from(now),
        organizationId,
        accountId);
    return find(organizationId, accountId);
  }

  @Transactional
  GmailSenderAccount revoke(UUID organizationId, UUID accountId, Instant now) {
    int changed =
        jdbcTemplate.update(
            """
            UPDATE integration_connection
               SET version = version + 1, status = 'REVOKED', is_default = FALSE,
                   encrypted_credential = NULL, credential_key_id = NULL,
                   credential_nonce = NULL, revoked_at = ?, disconnected_at = ?,
                   last_checked_at = ?, last_error_at = NULL, last_error_summary = NULL,
                   updated_at = ?
             WHERE organization_id = ? AND id = ? AND provider = 'GMAIL'
            """,
            Timestamp.from(now),
            Timestamp.from(now),
            Timestamp.from(now),
            Timestamp.from(now),
            organizationId,
            accountId);
    return changed == 1 ? find(organizationId, accountId) : null;
  }

  @Transactional
  void markReauthRequired(UUID organizationId, UUID accountId, Instant now) {
    updateError(
        organizationId, accountId, "REAUTH_REQUIRED", "Google authorization must be renewed", now);
  }

  @Transactional
  void markError(UUID organizationId, UUID accountId, String summary, Instant now) {
    updateError(organizationId, accountId, "ERROR", summary, now);
  }

  private void updateError(
      UUID organizationId, UUID accountId, String status, String summary, Instant now) {
    jdbcTemplate.update(
        """
        UPDATE integration_connection
           SET version = version + 1, status = ?, last_error_at = ?, last_error_summary = ?,
               last_checked_at = ?, updated_at = ?
         WHERE organization_id = ? AND id = ? AND provider = 'GMAIL' AND status <> 'REVOKED'
        """,
        status,
        Timestamp.from(now),
        summary,
        Timestamp.from(now),
        Timestamp.from(now),
        organizationId,
        accountId);
  }

  private void lockOrganization(UUID organizationId) {
    jdbcTemplate.query(
        "SELECT pg_advisory_xact_lock(hashtextextended(?::text, 0))",
        resultSet -> null,
        organizationId.toString());
  }

  private GmailSenderAccount map(ResultSet resultSet, int ignored) throws SQLException {
    byte[] encrypted = resultSet.getBytes("encrypted_credential");
    byte[] nonce = resultSet.getBytes("credential_nonce");
    String keyId = resultSet.getString("credential_key_id");
    GmailTokenCipher.EncryptedSecret secret =
        encrypted == null || nonce == null || keyId == null
            ? null
            : new GmailTokenCipher.EncryptedSecret(encrypted, nonce, keyId);
    return new GmailSenderAccount(
        resultSet.getObject("id", UUID.class),
        resultSet.getLong("version"),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getString("email_address"),
        resultSet.getString("normalized_email"),
        resultSet.getString("display_name"),
        GmailSenderAccountStatus.valueOf(resultSet.getString("status")),
        resultSet.getBoolean("is_default"),
        scopes(resultSet.getArray("granted_scopes")),
        secret,
        resultSet.getObject("connected_by", UUID.class),
        instant(resultSet, "connected_at"),
        instant(resultSet, "verified_at"),
        instant(resultSet, "revoked_at"),
        resultSet.getString("last_error_summary"),
        resultSet.getInt("daily_limit"),
        resultSet.getInt("min_interval_seconds"),
        instant(resultSet, "next_send_at"));
  }

  private Set<String> scopes(Array array) throws SQLException {
    return array == null ? Set.of() : Set.copyOf(Arrays.asList((String[]) array.getArray()));
  }

  private String scopes(Set<String> values) {
    return String.join(",", values.stream().sorted().toList());
  }

  private Instant instant(ResultSet resultSet, String column) throws SQLException {
    Timestamp value = resultSet.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
