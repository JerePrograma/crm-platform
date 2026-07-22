package com.gestudio.crm.settings;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.security.CurrentActor;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationSettingsService {

  private static final Set<String> LOCALES = Set.of("es-AR", "es", "en-US", "en");

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final AuditEventWriter auditEventWriter;
  private final SendingProperties environmentSending;

  public OrganizationSettingsService(
      JdbcTemplate jdbcTemplate,
      CurrentActor currentActor,
      AuditEventWriter auditEventWriter,
      SendingProperties environmentSending) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.auditEventWriter = auditEventWriter;
    this.environmentSending = environmentSending;
  }

  @Transactional(readOnly = true)
  public SettingsView get() {
    return jdbcTemplate.queryForObject(
        """
        SELECT version, name, timezone, currency, locale, branding_primary_color,
          follow_up_days, operating_window_start, operating_window_end,
          business_days, campaign_daily_limit
        FROM organization WHERE id = ? AND active = TRUE
        """,
        this::view,
        currentActor.organizationId());
  }

  @Transactional
  public SettingsView update(UpdateCommand command) {
    validate(command);
    boolean sendingOverrideRejected =
        Boolean.TRUE.equals(command.sendingEnabled())
            || Boolean.FALSE.equals(command.sendingDryRun())
            || command.sendingDailyLimit() != null && command.sendingDailyLimit() != 0
            || Boolean.FALSE.equals(command.sendingKillSwitch());
    int updated =
        jdbcTemplate.update(
            """
            UPDATE organization SET name = ?, timezone = ?, currency = ?, locale = ?,
              branding_primary_color = ?, follow_up_days = ?, operating_window_start = ?,
              operating_window_end = ?, business_days = CAST(? AS smallint[]),
              campaign_daily_limit = 0, version = version + 1, updated_at = now()
            WHERE id = ? AND version = ? AND active = TRUE
            """,
            command.name().trim(),
            command.timezone(),
            command.currency().toUpperCase(Locale.ROOT),
            command.locale(),
            command.brandingPrimaryColor(),
            command.followUpDays(),
            LocalTime.parse(command.operatingWindowStart()),
            LocalTime.parse(command.operatingWindowEnd()),
            arrayLiteral(command.businessDays()),
            currentActor.organizationId(),
            command.version());
    if (updated != 1) {
      throw new OptimisticConflictException("Organization settings changed concurrently");
    }
    auditEventWriter.record(
        sendingOverrideRejected
            ? "ORGANIZATION_SETTINGS_UPDATED_WITH_SENDING_OVERRIDE_REJECTED"
            : "ORGANIZATION_SETTINGS_UPDATED",
        "ORGANIZATION",
        currentActor.organizationId(),
        Map.of(
            "timezone", command.timezone(),
            "currency", command.currency().toUpperCase(Locale.ROOT),
            "locale", command.locale(),
            "sendingOverrideRejected", sendingOverrideRejected));
    return withRejected(get(), sendingOverrideRejected);
  }

  private SettingsView view(ResultSet rs, int row) throws SQLException {
    return new SettingsView(
        rs.getLong("version"),
        rs.getString("name"),
        rs.getString("timezone"),
        rs.getString("currency").trim(),
        rs.getString("locale"),
        rs.getString("branding_primary_color"),
        rs.getInt("follow_up_days"),
        rs.getTime("operating_window_start").toLocalTime().toString(),
        rs.getTime("operating_window_end").toLocalTime().toString(),
        days(rs.getArray("business_days")),
        rs.getInt("campaign_daily_limit"),
        false,
        new SendingGuardView(
            environmentSending.enabled(),
            environmentSending.dryRun(),
            environmentSending.dailyLimit(),
            environmentSending.environmentKillSwitch(),
            persistent("sending.enabled", "false"),
            persistent("sending.dry-run", "true"),
            Integer.parseInt(persistent("sending.daily-limit", "0")),
            persistent("sending.kill-switch", "true")));
  }

  private SettingsView withRejected(SettingsView view, boolean rejected) {
    return new SettingsView(
        view.version(),
        view.name(),
        view.timezone(),
        view.currency(),
        view.locale(),
        view.brandingPrimaryColor(),
        view.followUpDays(),
        view.operatingWindowStart(),
        view.operatingWindowEnd(),
        view.businessDays(),
        view.campaignDailyLimit(),
        rejected,
        view.sending());
  }

  private String persistent(String key, String fallback) {
    return jdbcTemplate
        .query(
            "SELECT setting_value FROM system_setting WHERE organization_id = ? AND setting_key = ?",
            (rs, row) -> rs.getString(1),
            currentActor.organizationId(),
            key)
        .stream()
        .findFirst()
        .orElse(fallback);
  }

  private List<Integer> days(Array array) throws SQLException {
    Number[] values = (Number[]) array.getArray();
    return Arrays.stream(values).map(Number::intValue).toList();
  }

  private String arrayLiteral(List<Integer> days) {
    return "{" + String.join(",", days.stream().map(String::valueOf).toList()) + "}";
  }

  private void validate(UpdateCommand command) {
    if (command == null || command.name() == null || command.name().isBlank()) {
      throw new IllegalArgumentException("Organization name is required");
    }
    if (command.name().trim().length() > 200) {
      throw new IllegalArgumentException("Organization name is too long");
    }
    if (command.timezone() == null || command.currency() == null || command.locale() == null) {
      throw new IllegalArgumentException("Timezone, currency, and locale are required");
    }
    ZoneId.of(command.timezone());
    Currency.getInstance(command.currency().toUpperCase(Locale.ROOT));
    if (!LOCALES.contains(command.locale())) {
      throw new IllegalArgumentException("Organization locale is not supported");
    }
    if (command.brandingPrimaryColor() == null
        || !command.brandingPrimaryColor().matches("#[0-9A-Fa-f]{6}")) {
      throw new IllegalArgumentException("Branding color must be a six-digit hex color");
    }
    if (command.followUpDays() < 1 || command.followUpDays() > 365) {
      throw new IllegalArgumentException("Follow-up days must be between 1 and 365");
    }
    LocalTime start = LocalTime.parse(command.operatingWindowStart());
    LocalTime end = LocalTime.parse(command.operatingWindowEnd());
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException("Operating window start must be before end");
    }
    if (command.businessDays() == null
        || command.businessDays().isEmpty()
        || command.businessDays().stream().distinct().count() != command.businessDays().size()
        || command.businessDays().stream().anyMatch(day -> day < 1 || day > 7)) {
      throw new IllegalArgumentException("Business days must be unique ISO weekdays 1 through 7");
    }
  }

  public record UpdateCommand(
      long version,
      String name,
      String timezone,
      String currency,
      String locale,
      String brandingPrimaryColor,
      int followUpDays,
      String operatingWindowStart,
      String operatingWindowEnd,
      List<Integer> businessDays,
      Boolean sendingEnabled,
      Boolean sendingDryRun,
      Integer sendingDailyLimit,
      Boolean sendingKillSwitch) {}

  public record SettingsView(
      long version,
      String name,
      String timezone,
      String currency,
      String locale,
      String brandingPrimaryColor,
      int followUpDays,
      String operatingWindowStart,
      String operatingWindowEnd,
      List<Integer> businessDays,
      int campaignDailyLimit,
      boolean sendingOverrideRejected,
      SendingGuardView sending) {}

  public record SendingGuardView(
      boolean environmentEnabled,
      boolean environmentDryRun,
      int environmentDailyLimit,
      boolean environmentKillSwitch,
      String databaseEnabled,
      String databaseDryRun,
      int databaseDailyLimit,
      String databaseKillSwitch) {}
}
