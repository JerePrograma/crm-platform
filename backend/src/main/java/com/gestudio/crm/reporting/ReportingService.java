package com.gestudio.crm.reporting;

import com.gestudio.crm.common.CsvSafety;
import com.gestudio.crm.security.CurrentActor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final Clock clock;

  public ReportingService(JdbcTemplate jdbcTemplate, CurrentActor currentActor, Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public DashboardReport dashboard(LocalDate from, LocalDate to) {
    DateRange range = range(from, to);
    UUID organizationId = currentActor.organizationId();
    Map<String, Long> prospectByStatus =
        counts(
            "SELECT status AS label, count(*) AS total FROM prospect WHERE organization_id = ? AND archived_at IS NULL GROUP BY status ORDER BY status",
            organizationId);
    Map<String, Long> prospectBySource =
        counts(
            "SELECT coalesce(source, 'UNSPECIFIED') AS label, count(*) AS total FROM prospect WHERE organization_id = ? AND archived_at IS NULL GROUP BY source ORDER BY label",
            organizationId);
    Map<String, Long> prospectByOwner =
        counts(
            "SELECT coalesce(u.display_name, 'UNASSIGNED') AS label, count(*) AS total FROM prospect p LEFT JOIN app_user u ON u.id = p.owner_user_id WHERE p.organization_id = ? AND p.archived_at IS NULL GROUP BY u.display_name ORDER BY label",
            organizationId);
    Map<String, Long> tasks =
        oneRowCounts(
            """
            SELECT count(*) FILTER (WHERE status IN ('OPEN', 'IN_PROGRESS')) AS open,
              count(*) FILTER (WHERE status IN ('OPEN', 'IN_PROGRESS') AND due_at >= ? AND due_at < ?) AS today,
              count(*) FILTER (WHERE status IN ('OPEN', 'IN_PROGRESS') AND due_at < ?) AS overdue,
              count(*) FILTER (WHERE status IN ('OPEN', 'IN_PROGRESS') AND due_at >= ? AND due_at < ?) AS upcoming
            FROM crm_task WHERE organization_id = ?
            """,
            range.todayStart(),
            range.tomorrowStart(),
            range.todayStart(),
            range.tomorrowStart(),
            range.toExclusive(),
            organizationId);
    Map<String, Long> activitiesByChannel =
        counts(
            "SELECT coalesce(channel, 'INTERNAL') AS label, count(*) AS total FROM activity WHERE organization_id = ? AND occurred_at >= ? AND occurred_at < ? GROUP BY channel ORDER BY label",
            organizationId,
            range.fromInclusive(),
            range.toExclusive());
    Map<String, Long> activityOutcomes =
        counts(
            "SELECT coalesce(outcome, activity_type) AS label, count(*) AS total FROM activity WHERE organization_id = ? AND occurred_at >= ? AND occurred_at < ? GROUP BY coalesce(outcome, activity_type) ORDER BY label",
            organizationId,
            range.fromInclusive(),
            range.toExclusive());
    Map<String, Long> opportunitiesByStage =
        counts(
            "SELECT stage AS label, count(*) AS total FROM opportunity WHERE organization_id = ? GROUP BY stage ORDER BY stage",
            organizationId);
    List<CurrencyTotal> opportunityValues =
        jdbcTemplate.query(
            """
            SELECT currency, count(*) AS opportunity_count,
              coalesce(sum(estimated_value), 0) AS total_value,
              coalesce(sum(estimated_value * probability / 100.0), 0) AS weighted_value
            FROM opportunity WHERE organization_id = ? AND stage NOT IN ('WON', 'LOST')
            GROUP BY currency ORDER BY currency
            """,
            (rs, row) ->
                new CurrencyTotal(
                    rs.getString("currency"),
                    rs.getLong("opportunity_count"),
                    rs.getBigDecimal("total_value").setScale(2, RoundingMode.HALF_UP),
                    rs.getBigDecimal("weighted_value").setScale(2, RoundingMode.HALF_UP)),
            organizationId);
    Map<String, Long> lossReasons =
        counts(
            "SELECT coalesce(lost_reason, 'UNSPECIFIED') AS label, count(*) AS total FROM opportunity WHERE organization_id = ? AND stage = 'LOST' GROUP BY lost_reason ORDER BY label",
            organizationId);
    Map<String, Long> campaigns =
        oneRowCounts(
            """
            SELECT count(*) FILTER (WHERE status = 'DRAFT') AS draft,
              count(*) FILTER (WHERE simulated_at >= ? AND simulated_at < ?) AS simulated,
              coalesce(sum(excluded_count), 0) AS excluded_recipients
            FROM campaign WHERE organization_id = ?
            """,
            range.fromInclusive(),
            range.toExclusive(),
            organizationId);
    Map<String, Long> outbox =
        counts(
            "SELECT status AS label, count(*) AS total FROM outbox_event WHERE organization_id = ? GROUP BY status ORDER BY status",
            organizationId);
    Map<String, Long> inbound =
        counts(
            "SELECT status AS label, count(*) AS total FROM inbound_message WHERE organization_id = ? GROUP BY status ORDER BY status",
            organizationId);
    Map<String, Long> prospectSummary =
        oneRowCounts(
            """
            SELECT count(*) FILTER (WHERE contact_eligible AND status NOT IN ('DO_NOT_CONTACT', 'CUSTOMER')) AS contactable,
              count(*) FILTER (WHERE NOT contact_eligible OR eligibility = 'EXCLUDED') AS excluded,
              count(*) FILTER (WHERE owner_user_id IS NULL) AS unowned,
              count(*) FILTER (WHERE status = 'REPLIED') AS replied,
              count(*) FILTER (WHERE status = 'INTERESTED') AS interested,
              count(*) FILTER (WHERE status IN ('DEMO_PROPOSED', 'DEMO_SCHEDULED')) AS demos,
              count(*) FILTER (WHERE status = 'PROPOSAL') AS proposals
            FROM prospect WHERE organization_id = ? AND archived_at IS NULL
            """,
            organizationId);
    Map<String, Long> opportunitySummary =
        oneRowCounts(
            """
            SELECT count(*) FILTER (WHERE stage = 'WON' AND actual_close_date >= ? AND actual_close_date <= ?) AS won,
              count(*) FILTER (WHERE stage = 'LOST' AND actual_close_date >= ? AND actual_close_date <= ?) AS lost,
              count(*) FILTER (WHERE stage NOT IN ('WON', 'LOST') AND stage_changed_at < CAST(? AS timestamptz) - interval '30 days') AS stalled
            FROM opportunity WHERE organization_id = ?
            """,
            range.from(),
            range.to(),
            range.from(),
            range.to(),
            range.toExclusive(),
            organizationId);
    Map<String, Long> operations = new LinkedHashMap<>();
    operations.put("deadLetter", outbox.getOrDefault("DEAD", 0L));
    operations.put(
        "workerFailures", outbox.getOrDefault("RETRY", 0L) + outbox.getOrDefault("DEAD", 0L));
    operations.put("inbound", inbound.values().stream().mapToLong(Long::longValue).sum());
    operations.put("quarantine", inbound.getOrDefault("QUARANTINED", 0L));
    operations.put(
        "disconnectedIntegrations",
        scalar(
            "SELECT count(*) FROM integration_connection WHERE organization_id = ? AND status <> 'CONNECTED'",
            organizationId));
    operations.put(
        "persistentKillSwitches",
        scalar(
            "SELECT count(*) FROM system_setting WHERE organization_id = ? AND setting_key = 'sending.kill-switch' AND setting_value = 'true'",
            organizationId));

    return new DashboardReport(
        range.from(),
        range.to(),
        range.zone().getId(),
        prospectByStatus,
        prospectBySource,
        prospectByOwner,
        prospectSummary,
        tasks,
        activitiesByChannel,
        activityOutcomes,
        opportunitiesByStage,
        opportunityValues,
        opportunitySummary,
        lossReasons,
        campaigns,
        outbox,
        inbound,
        operations);
  }

  public String csv(LocalDate from, LocalDate to) {
    DashboardReport report = dashboard(from, to);
    StringBuilder csv = new StringBuilder("section,metric,value,currency\r\n");
    append(csv, "prospect_status", report.prospectsByStatus());
    append(csv, "prospect_source", report.prospectsBySource());
    append(csv, "tasks", report.tasks());
    append(csv, "opportunity_stage", report.opportunitiesByStage());
    append(csv, "campaigns", report.campaigns());
    append(csv, "outbox", report.outbox());
    append(csv, "inbound", report.inbound());
    for (CurrencyTotal total : report.opportunityValues()) {
      csv.append("opportunity_value,total,")
          .append(total.totalValue())
          .append(',')
          .append(CsvSafety.cell(total.currency()))
          .append("\r\n");
      csv.append("opportunity_value,weighted,")
          .append(total.weightedValue())
          .append(',')
          .append(CsvSafety.cell(total.currency()))
          .append("\r\n");
    }
    return csv.toString();
  }

  private void append(StringBuilder csv, String section, Map<String, Long> values) {
    values.forEach(
        (label, value) ->
            csv.append(CsvSafety.cell(section))
                .append(',')
                .append(CsvSafety.cell(label))
                .append(',')
                .append(value)
                .append(",\r\n"));
  }

  private Map<String, Long> counts(String sql, Object... parameters) {
    LinkedHashMap<String, Long> result = new LinkedHashMap<>();
    jdbcTemplate
        .queryForList(sql, parameters)
        .forEach(
            row ->
                result.put(
                    String.valueOf(row.get("label")), ((Number) row.get("total")).longValue()));
    return result;
  }

  private Map<String, Long> oneRowCounts(String sql, Object... parameters) {
    Map<String, Object> row = jdbcTemplate.queryForMap(sql, parameters);
    LinkedHashMap<String, Long> result = new LinkedHashMap<>();
    row.forEach((key, value) -> result.put(key, ((Number) value).longValue()));
    return result;
  }

  private long scalar(String sql, Object... parameters) {
    Long count = jdbcTemplate.queryForObject(sql, Long.class, parameters);
    return count == null ? 0 : count;
  }

  private DateRange range(LocalDate requestedFrom, LocalDate requestedTo) {
    ZoneId zone =
        ZoneId.of(
            jdbcTemplate.queryForObject(
                "SELECT timezone FROM organization WHERE id = ?",
                String.class,
                currentActor.organizationId()));
    LocalDate today = LocalDate.now(clock.withZone(zone));
    LocalDate to = requestedTo == null ? today : requestedTo;
    LocalDate from = requestedFrom == null ? to.minusDays(29) : requestedFrom;
    if (from.isAfter(to) || from.isBefore(to.minusDays(365))) {
      throw new IllegalArgumentException("Report range must contain between 1 and 366 days");
    }
    Timestamp fromInclusive = Timestamp.from(from.atStartOfDay(zone).toInstant());
    Timestamp toExclusive = Timestamp.from(to.plusDays(1).atStartOfDay(zone).toInstant());
    return new DateRange(
        from,
        to,
        zone,
        fromInclusive,
        toExclusive,
        Timestamp.from(today.atStartOfDay(zone).toInstant()),
        Timestamp.from(today.plusDays(1).atStartOfDay(zone).toInstant()));
  }

  private record DateRange(
      LocalDate from,
      LocalDate to,
      ZoneId zone,
      Timestamp fromInclusive,
      Timestamp toExclusive,
      Timestamp todayStart,
      Timestamp tomorrowStart) {}

  public record CurrencyTotal(
      String currency, long opportunityCount, BigDecimal totalValue, BigDecimal weightedValue) {}

  public record DashboardReport(
      LocalDate from,
      LocalDate to,
      String timezone,
      Map<String, Long> prospectsByStatus,
      Map<String, Long> prospectsBySource,
      Map<String, Long> prospectsByOwner,
      Map<String, Long> prospectSummary,
      Map<String, Long> tasks,
      Map<String, Long> activitiesByChannel,
      Map<String, Long> activityOutcomes,
      Map<String, Long> opportunitiesByStage,
      List<CurrencyTotal> opportunityValues,
      Map<String, Long> opportunitySummary,
      Map<String, Long> lossReasons,
      Map<String, Long> campaigns,
      Map<String, Long> outbox,
      Map<String, Long> inbound,
      Map<String, Long> operations) {}
}
