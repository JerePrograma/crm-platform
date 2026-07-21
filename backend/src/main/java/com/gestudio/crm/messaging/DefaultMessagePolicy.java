package com.gestudio.crm.messaging;

import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.security.CurrentActor;
import com.gestudio.crm.settings.SendingProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultMessagePolicy implements MessagePolicy {

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final SendingProperties sendingProperties;

  public DefaultMessagePolicy(
      JdbcTemplate jdbcTemplate, CurrentActor currentActor, SendingProperties sendingProperties) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.sendingProperties = sendingProperties;
  }

  @Override
  @Transactional(readOnly = true)
  public PolicyDecision evaluate(
      UUID prospectId, UUID contactId, String channel, boolean realSend) {
    String normalizedChannel = channel == null ? "" : channel.toUpperCase(java.util.Locale.ROOT);
    if (!Set.of("EMAIL", "WHATSAPP").contains(normalizedChannel)) {
      throw new IllegalArgumentException("Message channel must be EMAIL or WHATSAPP");
    }
    Recipient recipient =
        jdbcTemplate
            .query(
                """
                SELECT p.contact_eligible, p.eligibility, p.status,
                  cc.id AS channel_id, cc.normalized_value,
                  EXISTS (
                    SELECT 1 FROM exclusion e
                    WHERE e.organization_id = p.organization_id
                      AND e.channel_type = cc.type AND e.normalized_value = cc.normalized_value
                  ) AS excluded
                FROM prospect p
                JOIN contact c ON c.organization_id = p.organization_id
                  AND c.institution_id = p.institution_id AND c.id = ? AND c.deleted_at IS NULL
                JOIN contact_channel cc ON cc.organization_id = c.organization_id
                  AND cc.contact_id = c.id AND cc.type = ? AND cc.valid
                WHERE p.organization_id = ? AND p.id = ? AND p.archived_at IS NULL
                  AND c.consent <> 'DENIED' AND cc.consent <> 'DENIED'
                ORDER BY cc.preferred DESC, cc.primary_channel DESC, cc.created_at
                LIMIT 1
                """,
                this::recipient,
                contactId,
                normalizedChannel,
                currentActor.organizationId(),
                prospectId)
            .stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Valid contact channel was not found for this prospect"));
    if (!recipient.contactEligible()
        || !"ELIGIBLE".equals(recipient.eligibility())
        || Set.of("CUSTOMER", "DO_NOT_CONTACT", "ARCHIVED").contains(recipient.status())) {
      return new PolicyDecision(
          false, false, "BLOCKED_BY_POLICY", recipient.channelId(), recipient.normalizedValue());
    }
    if (recipient.excluded()) {
      return new PolicyDecision(
          false, false, "BLOCKED_BY_EXCLUSION", recipient.channelId(), recipient.normalizedValue());
    }
    Map<String, String> database = settings();
    boolean persistentKill = !"false".equalsIgnoreCase(database.get("sending.kill-switch"));
    boolean persistentEnabled = "true".equalsIgnoreCase(database.get("sending.enabled"));
    boolean persistentDryRun = !"false".equalsIgnoreCase(database.get("sending.dry-run"));
    int persistentLimit = integer(database.get("sending.daily-limit"));
    boolean realAllowed =
        !sendingProperties.blocksRealSending()
            && !persistentKill
            && persistentEnabled
            && !persistentDryRun
            && persistentLimit > 0;
    String blockade =
        sendingProperties.environmentKillSwitch() || persistentKill
            ? "BLOCKED_BY_KILL_SWITCH"
            : realAllowed ? "ALLOWED" : "BLOCKED_BY_CONFIGURATION";
    return new PolicyDecision(
        true,
        realSend && realAllowed,
        realSend && realAllowed ? "ALLOWED" : blockade,
        recipient.channelId(),
        recipient.normalizedValue());
  }

  private Map<String, String> settings() {
    return jdbcTemplate.query(
        """
        SELECT setting_key, setting_value FROM system_setting
        WHERE organization_id = ? AND setting_key IN (
          'sending.kill-switch', 'sending.enabled', 'sending.dry-run', 'sending.daily-limit'
        )
        """,
        rs -> {
          Map<String, String> values = new LinkedHashMap<>();
          while (rs.next()) {
            values.put(rs.getString(1), rs.getString(2));
          }
          return values;
        },
        currentActor.organizationId());
  }

  private Recipient recipient(ResultSet rs, int rowNum) throws SQLException {
    return new Recipient(
        rs.getBoolean("contact_eligible"),
        rs.getString("eligibility"),
        rs.getString("status"),
        rs.getObject("channel_id", UUID.class),
        rs.getString("normalized_value"),
        rs.getBoolean("excluded"));
  }

  private int integer(String value) {
    try {
      return value == null ? 0 : Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      return 0;
    }
  }

  private record Recipient(
      boolean contactEligible,
      String eligibility,
      String status,
      UUID channelId,
      String normalizedValue,
      boolean excluded) {}
}
