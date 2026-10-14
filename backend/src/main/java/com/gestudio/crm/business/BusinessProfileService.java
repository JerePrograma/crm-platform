package com.gestudio.crm.business;

import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.identity.IdentityService;
import com.gestudio.crm.security.CurrentActor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessProfileService {

  private final JdbcTemplate jdbcTemplate;
  private final BusinessProfileProperties properties;
  private final NormalizationService normalizationService;
  private final IdentityService identityService;
  private final CurrentActor currentActor;

  public BusinessProfileService(
      JdbcTemplate jdbcTemplate,
      BusinessProfileProperties properties,
      NormalizationService normalizationService,
      IdentityService identityService,
      CurrentActor currentActor) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
    this.normalizationService = normalizationService;
    this.identityService = identityService;
    this.currentActor = currentActor;
  }

  @Transactional
  public void initializeIfRequired() {
    if (!properties.scalaris()) {
      return;
    }
    properties.marketingConfigured();
    lockBootstrap();
    UUID organizationId = properties.organizationId();
    jdbcTemplate.update(
        """
        INSERT INTO organization (id, created_at, updated_at, slug, name, branding_primary_color)
        VALUES (?, now(), now(), ?, ?, ?) ON CONFLICT (id) DO NOTHING
        """,
        organizationId,
        ScalarisCatalog.SLUG,
        ScalarisCatalog.NAME,
        ScalarisCatalog.BRAND_COLOR);
    String slug =
        jdbcTemplate.queryForObject(
            "SELECT slug FROM organization WHERE id = ?", String.class, organizationId);
    if (!ScalarisCatalog.SLUG.equals(slug)) {
      throw new IllegalStateException("The Scalaris organization ID is already in use");
    }
    for (String role : List.of("ADMIN", "MANAGER", "SALES", "VIEWER")) {
      UUID roleId = roleId(role);
      insertRole(roleId, role, "Rol " + role + " de Scalaris");
      jdbcTemplate.update(
          """
          INSERT INTO role_permission (role_id, permission_code)
          SELECT ?, p.permission_code FROM role_permission p
          JOIN crm_role r ON r.id = p.role_id
          WHERE r.organization_id = ? AND r.name = ? ON CONFLICT DO NOTHING
          """,
          roleId,
          com.gestudio.crm.common.TenantIds.BOOTSTRAP_ORGANIZATION_ID,
          role);
    }
    UUID marketingRole = roleId("MARKETING");
    insertRole(
        marketingRole,
        "MARKETING",
        "Operación de Marketing sin administración técnica ni envío real");
    for (String permission : ScalarisCatalog.MARKETING_PERMISSIONS) {
      jdbcTemplate.update(
          "INSERT INTO role_permission (role_id, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING",
          marketingRole,
          permission);
    }
    Map.of(
            "sending.enabled",
            "false",
            "sending.dry-run",
            "true",
            "sending.daily-limit",
            "0",
            "sending.kill-switch",
            "true")
        .forEach(
            (key, value) ->
                jdbcTemplate.update(
                    """
            INSERT INTO system_setting (id, created_at, updated_at, organization_id, setting_key, setting_value)
            VALUES (?, now(), now(), ?, ?, ?) ON CONFLICT (organization_id, setting_key) DO NOTHING
            """,
                    seedId("setting:" + key),
                    organizationId,
                    key,
                    value));
    for (ScalarisCatalog.TagSeed tag : ScalarisCatalog.TAGS) {
      jdbcTemplate.update(
          """
          INSERT INTO crm_tag (id, organization_id, name, normalized_name, color, created_at, updated_at)
          VALUES (?, ?, ?, ?, ?, now(), now()) ON CONFLICT DO NOTHING
          """,
          seedId("tag:" + tag.name()),
          organizationId,
          tag.name(),
          normalizationService.normalizeName(tag.name()),
          tag.color());
    }
    for (ScalarisCatalog.TemplateSeed template : ScalarisCatalog.TEMPLATES) {
      UUID templateId = seedId("template:" + template.name());
      int inserted =
          jdbcTemplate.update(
              """
          INSERT INTO email_template (id, organization_id, name, channel, created_at, updated_at)
          VALUES (?, ?, ?, ?, now(), now()) ON CONFLICT DO NOTHING
          """,
              templateId,
              organizationId,
              template.name(),
              template.channel());
      if (inserted == 1) {
        jdbcTemplate.update(
            """
            INSERT INTO template_version (id, organization_id, template_id, version_number, subject,
              text_body, html_body, variables, created_at, updated_at)
            VALUES (?, ?, ?, 1, ?, ?, '', '[]'::jsonb, now(), now())
            """,
            seedId("template-version:" + template.name()),
            organizationId,
            templateId,
            template.subject(),
            template.body());
      }
    }
  }

  @Transactional
  public void bootstrapMarketingIfRequired() {
    if (!properties.scalaris() || !properties.marketingConfigured()) {
      return;
    }
    lockBootstrap();
    String normalizedUsername = properties.marketingUsername().trim().toLowerCase(Locale.ROOT);
    List<ExistingUser> existing =
        jdbcTemplate.query(
            """
        SELECT u.id, m.organization_id, r.name
        FROM app_user u LEFT JOIN organization_membership m ON m.user_id = u.id
        LEFT JOIN crm_role r ON r.id = m.role_id WHERE u.normalized_username = ?
        """,
            (rs, row) ->
                new ExistingUser(
                    rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3)),
            normalizedUsername);
    if (!existing.isEmpty()) {
      if (existing.size() != 1
          || !properties.organizationId().equals(existing.getFirst().organizationId())
          || !"MARKETING".equals(existing.getFirst().role())) {
        throw new IllegalStateException(
            "The configured Marketing username is already assigned to another account");
      }
      return;
    }
    identityService.createUser(
        properties.organizationId(),
        properties.marketingUsername(),
        properties.effectiveMarketingDisplayName(),
        properties.marketingPassword(),
        "MARKETING",
        null);
  }

  @Transactional(readOnly = true)
  public ProfileView get() {
    UUID organizationId = currentActor.requiredPrincipal().organizationId();
    String name =
        jdbcTemplate.queryForObject(
            "SELECT name FROM organization WHERE id = ? AND active = TRUE",
            String.class,
            organizationId);
    if (BusinessProfileProperties.SCALARIS_ORGANIZATION_ID.equals(organizationId)) {
      return new ProfileView(
          "SCALARIS",
          name,
          ScalarisCatalog.TAGLINE,
          ScalarisCatalog.DESCRIPTOR,
          ScalarisCatalog.WEBSITE,
          ScalarisCatalog.WHATSAPP_DISPLAY,
          ScalarisCatalog.WHATSAPP_URL,
          ScalarisCatalog.COVERAGE,
          ScalarisCatalog.SERVICE_LINES,
          ScalarisCatalog.OPERATING_GUIDANCE,
          true);
    }
    return new ProfileView("LEGACY", name, "", "", "", "", "", "", List.of(), List.of(), true);
  }

  private void lockBootstrap() {
    jdbcTemplate.execute("SELECT pg_advisory_xact_lock(hashtext('crm:scalaris-local:bootstrap'))");
  }

  private void insertRole(UUID id, String name, String description) {
    jdbcTemplate.update(
        "INSERT INTO crm_role (id, organization_id, name, description) VALUES (?, ?, ?, ?) ON CONFLICT (organization_id, name) DO NOTHING",
        id,
        properties.organizationId(),
        name,
        description);
  }

  private UUID roleId(String role) {
    return jdbcTemplate
        .query(
            "SELECT id FROM crm_role WHERE organization_id = ? AND name = ?",
            (rs, row) -> rs.getObject(1, UUID.class),
            properties.organizationId(),
            role)
        .stream()
        .findFirst()
        .orElseGet(() -> seedId("role:" + role));
  }

  private UUID seedId(String key) {
    return UUID.nameUUIDFromBytes(("scalaris-local:" + key).getBytes(StandardCharsets.UTF_8));
  }

  private record ExistingUser(UUID id, UUID organizationId, String role) {}

  public record ProfileView(
      String code,
      String organizationName,
      String tagline,
      String descriptor,
      String website,
      String whatsappDisplay,
      String whatsappUrl,
      String coverage,
      List<String> serviceLines,
      List<String> operatingGuidance,
      boolean manualCommunicationsOnly) {}
}
