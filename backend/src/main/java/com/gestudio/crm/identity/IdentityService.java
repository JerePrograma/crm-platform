package com.gestudio.crm.identity;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.DuplicateResourceException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.security.SecurityBootstrapProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService implements UserDetailsService {

  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final String PRINCIPAL_SQL =
      """
      SELECT u.id, u.username, u.display_name, u.password_hash, u.active,
             u.locked_until, m.organization_id, r.name AS role_name,
             string_agg(DISTINCT rp.permission_code, ',') AS permission_codes
      FROM app_user u
      JOIN organization_membership m ON m.user_id = u.id AND m.active = TRUE
      JOIN organization o ON o.id = m.organization_id AND o.active = TRUE
      JOIN crm_role r ON r.id = m.role_id AND r.organization_id = m.organization_id
      LEFT JOIN role_permission rp ON rp.role_id = r.id
      WHERE u.normalized_username = ?
      GROUP BY u.id, u.username, u.display_name, u.password_hash, u.active,
               u.locked_until, m.organization_id, r.name, m.created_at
      ORDER BY m.created_at
      LIMIT 1
      """;

  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;
  private final SecurityBootstrapProperties bootstrapProperties;
  private final AuditEventWriter auditEventWriter;

  public IdentityService(
      JdbcTemplate jdbcTemplate,
      PasswordEncoder passwordEncoder,
      SecurityBootstrapProperties bootstrapProperties,
      AuditEventWriter auditEventWriter) {
    this.jdbcTemplate = jdbcTemplate;
    this.passwordEncoder = passwordEncoder;
    this.bootstrapProperties = bootstrapProperties;
    this.auditEventWriter = auditEventWriter;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    List<CrmPrincipal> matches =
        jdbcTemplate.query(PRINCIPAL_SQL, this::principal, normalizeUsername(username));
    if (matches.isEmpty()) {
      throw new UsernameNotFoundException("Invalid username or password");
    }
    return matches.getFirst();
  }

  @Transactional
  public void bootstrapIfRequired() {
    Integer administrators =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM app_user u
            JOIN organization_membership m ON m.user_id = u.id AND m.active = TRUE
            JOIN crm_role r ON r.id = m.role_id
            WHERE u.active = TRUE AND r.name = 'ADMIN'
            """,
            Integer.class);
    if (administrators != null && administrators > 0) {
      return;
    }
    if (!bootstrapProperties.configured()) {
      return;
    }
    createUser(
        com.gestudio.crm.common.TenantIds.BOOTSTRAP_ORGANIZATION_ID,
        bootstrapProperties.username(),
        bootstrapProperties.username(),
        bootstrapProperties.password(),
        "ADMIN",
        null);
  }

  @Transactional
  public UserView createUser(
      UUID organizationId,
      String username,
      String displayName,
      String rawPassword,
      String role,
      UUID actorUserId) {
    String normalized = normalizeUsername(username);
    String safeDisplayName = required(displayName, "Display name");
    validatePassword(rawPassword);
    String normalizedRole = required(role, "Role").toUpperCase(Locale.ROOT);
    UUID roleId =
        jdbcTemplate
            .query(
                "SELECT id FROM crm_role WHERE organization_id = ? AND name = ?",
                (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
                organizationId,
                normalizedRole)
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + normalizedRole));
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();
    try {
      jdbcTemplate.update(
          """
          INSERT INTO app_user (
            id, created_at, updated_at, username, normalized_username, display_name,
            password_hash, active, password_changed_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, ?)
          """,
          userId,
          java.sql.Timestamp.from(now),
          java.sql.Timestamp.from(now),
          username.trim(),
          normalized,
          safeDisplayName,
          passwordEncoder.encode(rawPassword),
          java.sql.Timestamp.from(now));
      jdbcTemplate.update(
          """
          INSERT INTO organization_membership (
            user_id, organization_id, role_id, active, created_at, updated_at
          ) VALUES (?, ?, ?, TRUE, ?, ?)
          """,
          userId,
          organizationId,
          roleId,
          java.sql.Timestamp.from(now),
          java.sql.Timestamp.from(now));
    } catch (DuplicateKeyException exception) {
      throw new DuplicateResourceException("Username already exists");
    }
    auditEventWriter.recordFor(
        organizationId,
        actorUserId,
        "USER_CREATED",
        "User",
        userId,
        "SUCCESS",
        Map.of("role", normalizedRole));
    return new UserView(userId, username.trim(), safeDisplayName, normalizedRole, true, now, null);
  }

  @Transactional(readOnly = true)
  public List<UserView> listUsers(UUID organizationId) {
    return jdbcTemplate.query(
        """
        SELECT u.id, u.username, u.display_name, r.name, u.active, u.created_at, u.last_login_at
        FROM app_user u
        JOIN organization_membership m ON m.user_id = u.id
        JOIN crm_role r ON r.id = m.role_id
        WHERE m.organization_id = ?
        ORDER BY lower(u.display_name), u.id
        """,
        (resultSet, rowNumber) ->
            new UserView(
                resultSet.getObject(1, UUID.class),
                resultSet.getString(2),
                resultSet.getString(3),
                resultSet.getString(4),
                resultSet.getBoolean(5),
                resultSet.getObject(6, OffsetDateTime.class).toInstant(),
                nullableInstant(resultSet, 7)),
        organizationId);
  }

  @Transactional
  public void setActive(UUID organizationId, UUID userId, boolean active, UUID actorUserId) {
    int updated =
        jdbcTemplate.update(
            """
            UPDATE app_user u
            SET active = ?, updated_at = now(), version = version + 1
            WHERE u.id = ?
              AND EXISTS (
                SELECT 1 FROM organization_membership m
                WHERE m.user_id = u.id AND m.organization_id = ?
              )
            """,
            active,
            userId,
            organizationId);
    if (updated == 0) {
      throw new ResourceNotFoundException("User not found: " + userId);
    }
    auditEventWriter.recordFor(
        organizationId,
        actorUserId,
        active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
        "User",
        userId,
        "SUCCESS",
        Map.of());
  }

  @Transactional
  public void changePassword(CrmPrincipal principal, String currentPassword, String newPassword) {
    if (!passwordEncoder.matches(currentPassword, principal.password())) {
      throw new IllegalArgumentException("Current password is incorrect");
    }
    replacePassword(
        principal.organizationId(), principal.userId(), newPassword, principal.userId());
  }

  @Transactional
  public void resetPassword(
      UUID organizationId, UUID userId, String newPassword, UUID actorUserId) {
    replacePassword(organizationId, userId, newPassword, actorUserId);
  }

  private void replacePassword(
      UUID organizationId, UUID userId, String newPassword, UUID actorUserId) {
    validatePassword(newPassword);
    int updated =
        jdbcTemplate.update(
            """
            UPDATE app_user u
            SET password_hash = ?, password_changed_at = now(), updated_at = now(),
                version = version + 1, failed_attempts = 0, locked_until = NULL
            WHERE u.id = ?
              AND EXISTS (
                SELECT 1 FROM organization_membership m
                WHERE m.user_id = u.id AND m.organization_id = ?
              )
            """,
            passwordEncoder.encode(newPassword),
            userId,
            organizationId);
    if (updated == 0) {
      throw new ResourceNotFoundException("User not found: " + userId);
    }
    auditEventWriter.recordFor(
        organizationId, actorUserId, "PASSWORD_CHANGED", "User", userId, "SUCCESS", Map.of());
  }

  @Transactional
  public void recordFailedLogin(String username) {
    List<UserReference> users = userReference(username);
    if (users.isEmpty()) {
      return;
    }
    UserReference user = users.getFirst();
    jdbcTemplate.update(
        """
        UPDATE app_user
        SET failed_attempts = failed_attempts + 1,
            locked_until = CASE WHEN failed_attempts + 1 >= ? THEN now() + interval '15 minutes' ELSE locked_until END,
            updated_at = now(), version = version + 1
        WHERE id = ?
        """,
        MAX_FAILED_ATTEMPTS,
        user.userId());
    auditEventWriter.recordFor(
        user.organizationId(),
        user.userId(),
        "LOGIN_FAILED",
        "User",
        user.userId(),
        "DENIED",
        Map.of());
  }

  @Transactional
  public void recordSuccessfulLogin(CrmPrincipal principal) {
    jdbcTemplate.update(
        """
        UPDATE app_user
        SET failed_attempts = 0, locked_until = NULL, last_login_at = now(),
            updated_at = now(), version = version + 1
        WHERE id = ?
        """,
        principal.userId());
    auditEventWriter.recordFor(
        principal.organizationId(),
        principal.userId(),
        "LOGIN_SUCCEEDED",
        "User",
        principal.userId(),
        "SUCCESS",
        Map.of());
  }

  @Transactional
  public void recordLogout(CrmPrincipal principal) {
    auditEventWriter.recordFor(
        principal.organizationId(),
        principal.userId(),
        "LOGOUT",
        "User",
        principal.userId(),
        "SUCCESS",
        Map.of());
  }

  @Transactional(readOnly = true)
  public boolean isSessionValid(CrmPrincipal principal) {
    Integer matches =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM app_user u
            JOIN organization_membership m ON m.user_id = u.id
            WHERE u.id = ? AND m.organization_id = ? AND u.active = TRUE AND m.active = TRUE
              AND (u.locked_until IS NULL OR u.locked_until <= now())
              AND u.password_hash = ?
            """,
            Integer.class,
            principal.userId(),
            principal.organizationId(),
            principal.password());
    return matches != null && matches == 1;
  }

  private CrmPrincipal principal(ResultSet resultSet, int rowNumber) throws SQLException {
    String codes = resultSet.getString("permission_codes");
    Set<String> permissions =
        codes == null || codes.isBlank()
            ? Set.of()
            : new LinkedHashSet<>(Arrays.asList(codes.split(",")));
    return new CrmPrincipal(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getString("username"),
        resultSet.getString("display_name"),
        resultSet.getString("password_hash"),
        resultSet.getString("role_name"),
        permissions,
        resultSet.getBoolean("active"),
        nullableInstant(resultSet, "locked_until"));
  }

  private List<UserReference> userReference(String username) {
    return jdbcTemplate.query(
        """
        SELECT u.id, m.organization_id
        FROM app_user u
        JOIN organization_membership m ON m.user_id = u.id
        WHERE u.normalized_username = ?
        ORDER BY m.created_at
        LIMIT 1
        """,
        (resultSet, rowNumber) ->
            new UserReference(
                resultSet.getObject(1, UUID.class), resultSet.getObject(2, UUID.class)),
        normalizeUsername(username));
  }

  private String normalizeUsername(String username) {
    return required(username, "Username").toLowerCase(Locale.ROOT);
  }

  private String required(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value.trim();
  }

  private void validatePassword(String password) {
    if (password == null
        || password.length() < bootstrapProperties.effectiveMinimumPasswordLength()) {
      throw new IllegalArgumentException(
          "Password must contain at least "
              + bootstrapProperties.effectiveMinimumPasswordLength()
              + " characters");
    }
    if (password.chars().allMatch(Character::isLetter)
        || password.chars().allMatch(Character::isDigit)) {
      throw new IllegalArgumentException("Password must combine letters and non-letter characters");
    }
  }

  private Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
    OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private Instant nullableInstant(ResultSet resultSet, int column) throws SQLException {
    OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private record UserReference(UUID userId, UUID organizationId) {}

  public record UserView(
      UUID id,
      String username,
      String displayName,
      String role,
      boolean active,
      Instant createdAt,
      Instant lastLoginAt) {}
}
