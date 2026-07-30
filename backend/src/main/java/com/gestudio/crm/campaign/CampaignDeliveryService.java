package com.gestudio.crm.campaign;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.campaign.SafeTemplateRenderer.RenderedTemplate;
import com.gestudio.crm.common.CorrelationIds;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.common.UnprocessableEntityException;
import com.gestudio.crm.gmail.GmailDeliveryProperties;
import com.gestudio.crm.gmail.UnsubscribeTokenService;
import com.gestudio.crm.gmail.UnsubscribeTokenService.IssuedToken;
import com.gestudio.crm.messaging.MessagingProperties;
import com.gestudio.crm.outbox.OutboxPublisher;
import com.gestudio.crm.outbox.OutboxPublisher.PublishCommand;
import com.gestudio.crm.security.CurrentActor;
import com.gestudio.crm.settings.SendingProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignDeliveryService {

  public static final String LIVE_CONFIRMATION = "SEND_LIVE_CAMPAIGN";

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final AuditEventWriter auditEventWriter;
  private final SafeTemplateRenderer renderer;
  private final OutboxPublisher outboxPublisher;
  private final UnsubscribeTokenService unsubscribeTokens;
  private final SendingProperties sendingProperties;
  private final MessagingProperties messagingProperties;
  private final GmailDeliveryProperties gmailProperties;
  private final Clock clock;

  public CampaignDeliveryService(
      JdbcTemplate jdbcTemplate,
      CurrentActor currentActor,
      AuditEventWriter auditEventWriter,
      SafeTemplateRenderer renderer,
      OutboxPublisher outboxPublisher,
      UnsubscribeTokenService unsubscribeTokens,
      SendingProperties sendingProperties,
      MessagingProperties messagingProperties,
      GmailDeliveryProperties gmailProperties,
      Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.auditEventWriter = auditEventWriter;
    this.renderer = renderer;
    this.outboxPublisher = outboxPublisher;
    this.unsubscribeTokens = unsubscribeTokens;
    this.sendingProperties = sendingProperties;
    this.messagingProperties = messagingProperties;
    this.gmailProperties = gmailProperties;
    this.clock = clock;
  }

  @Transactional
  public CampaignProgress schedule(
      UUID campaignId, long version, String confirmation, Instant scheduledAt) {
    Instant now = clock.instant();
    if (scheduledAt == null || scheduledAt.isBefore(now.minusSeconds(60))) {
      throw new IllegalArgumentException("Scheduled time must not be in the past");
    }
    if (scheduledAt.isAfter(now.plusSeconds(366L * 24 * 60 * 60))) {
      throw new IllegalArgumentException("Scheduled time must be within one year");
    }
    return activate(campaignId, version, confirmation, scheduledAt, false);
  }

  @Transactional
  public CampaignProgress start(UUID campaignId, long version, String confirmation) {
    return activate(campaignId, version, confirmation, clock.instant(), true);
  }

  private CampaignProgress activate(
      UUID campaignId, long version, String confirmation, Instant availableAt, boolean immediate) {
    requireConfirmation(confirmation);
    LiveCampaign campaign = liveCampaign(campaignId);
    if (campaign.version() != version) {
      throw new OptimisticConflictException("Campaign was modified by another user");
    }
    if (campaign.executionMode() != CampaignExecutionMode.LIVE) {
      throw new UnprocessableEntityException("Only live campaigns can be scheduled for dispatch");
    }
    if (campaign.status() != CampaignState.APPROVED || !campaign.approved()) {
      throw new UnprocessableEntityException("Campaign is not approved for live execution");
    }
    if (campaign.frozenAt() == null || campaign.recipientCount() <= 0) {
      throw new UnprocessableEntityException("Campaign audience is not frozen");
    }
    if (campaign.approvalFingerprint() == null) {
      throw new UnprocessableEntityException("Campaign approval is no longer valid");
    }
    requireLiveFlags(campaign);

    List<Recipient> recipients = recipients(campaign.id());
    if (recipients.size() != campaign.recipientCount()) {
      throw new UnprocessableEntityException("Frozen audience no longer matches the campaign");
    }
    String correlationId = CorrelationIds.currentOrCreate();
    for (Recipient recipient : recipients) {
      enqueue(campaign, recipient, availableAt, correlationId);
    }
    CampaignState target = immediate ? CampaignState.RUNNING : CampaignState.SCHEDULED;
    int updated =
        jdbcTemplate.update(
            """
            UPDATE campaign SET status = ?, scheduled_at = ?, executed_by = ?,
              started_at = CASE WHEN ? THEN now() ELSE started_at END,
              updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ? AND status = 'APPROVED'
            """,
            target.name(),
            Timestamp.from(availableAt),
            currentActor.userIdOrNull(),
            immediate,
            campaign.id(),
            currentActor.organizationId(),
            version);
    if (updated != 1) {
      throw new OptimisticConflictException("Campaign was modified by another user");
    }
    auditEventWriter.record(
        immediate ? "CAMPAIGN_LIVE_STARTED" : "CAMPAIGN_LIVE_SCHEDULED",
        "CAMPAIGN",
        campaign.id(),
        Map.of(
            "recipients", recipients.size(),
            "senderAccountId", campaign.senderAccountId(),
            "dailyLimit", campaign.dailyLimit(),
            "scheduledAt", availableAt.toString()));
    return progress(campaign.id());
  }

  @Transactional
  public CampaignProgress pause(UUID campaignId, long version) {
    return changeState(
        campaignId,
        version,
        Set.of(CampaignState.RUNNING, CampaignState.SCHEDULED),
        CampaignState.PAUSED,
        "CAMPAIGN_LIVE_PAUSED");
  }

  @Transactional
  public CampaignProgress resume(UUID campaignId, long version) {
    changeState(
        campaignId,
        version,
        Set.of(CampaignState.PAUSED),
        CampaignState.RUNNING,
        "CAMPAIGN_LIVE_RESUMED");
    jdbcTemplate.update(
        """
        UPDATE outbox_event SET status = 'RETRY', next_attempt_at = now(), processed_at = NULL,
          updated_at = now()
        WHERE organization_id = ?
          AND (
            status IN ('PENDING', 'RETRY')
            OR (status = 'BLOCKED' AND last_error_code = 'REAUTH_REQUIRED')
          )
          AND aggregate_id IN (
            SELECT id FROM message_record WHERE organization_id = ? AND campaign_id = ?
              AND status IN ('PENDING', 'SCHEDULED', 'RETRYABLE')
          )
        """,
        currentActor.organizationId(),
        currentActor.organizationId(),
        campaignId);
    return progress(campaignId);
  }

  @Transactional
  public CampaignProgress cancel(UUID campaignId, long version) {
    changeState(
        campaignId,
        version,
        Set.of(
            CampaignState.APPROVED,
            CampaignState.SCHEDULED,
            CampaignState.RUNNING,
            CampaignState.PAUSED),
        CampaignState.CANCELLED,
        "CAMPAIGN_LIVE_CANCELLED");
    jdbcTemplate.update(
        """
        UPDATE message_record SET status = 'CANCELLED', result_category = 'CAMPAIGN_CANCELLED',
          next_attempt_at = NULL, updated_at = now(), version = version + 1
        WHERE organization_id = ? AND campaign_id = ?
          AND status IN ('PENDING', 'SCHEDULED', 'RETRYABLE')
        """,
        currentActor.organizationId(),
        campaignId);
    jdbcTemplate.update(
        """
        UPDATE outbox_event SET status = 'CANCELLED', processed_at = now(), updated_at = now(),
          last_error_code = 'CAMPAIGN_CANCELLED', last_error_summary = 'Campaign cancelled'
        WHERE organization_id = ? AND status IN ('PENDING', 'RETRY')
          AND aggregate_id IN (
            SELECT id FROM message_record WHERE organization_id = ? AND campaign_id = ?
          )
        """,
        currentActor.organizationId(),
        currentActor.organizationId(),
        campaignId);
    return progress(campaignId);
  }

  private CampaignProgress changeState(
      UUID campaignId,
      long version,
      Set<CampaignState> allowed,
      CampaignState target,
      String auditAction) {
    LiveCampaign campaign = liveCampaign(campaignId);
    if (campaign.version() != version) {
      throw new OptimisticConflictException("Campaign was modified by another user");
    }
    if (campaign.executionMode() != CampaignExecutionMode.LIVE
        || !allowed.contains(campaign.status())) {
      throw new UnprocessableEntityException("Campaign state does not allow this action");
    }
    int updated =
        jdbcTemplate.update(
            """
            UPDATE campaign SET status = ?,
              paused_at = CASE WHEN ? = 'PAUSED' THEN now() ELSE paused_at END,
              cancelled_at = CASE WHEN ? = 'CANCELLED' THEN now() ELSE cancelled_at END,
              started_at = CASE WHEN ? = 'RUNNING' THEN COALESCE(started_at, now()) ELSE started_at END,
              updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            target.name(),
            target.name(),
            target.name(),
            target.name(),
            campaignId,
            currentActor.organizationId(),
            version);
    if (updated != 1) {
      throw new OptimisticConflictException("Campaign was modified by another user");
    }
    auditEventWriter.record(auditAction, "CAMPAIGN", campaignId, Map.of("state", target));
    return progress(campaignId);
  }

  @Transactional(readOnly = true)
  public CampaignProgress progress(UUID campaignId) {
    liveCampaign(campaignId);
    return jdbcTemplate.queryForObject(
        """
        SELECT c.id, c.status, c.version, c.recipient_count, c.excluded_count,
          count(m.id) FILTER (WHERE m.status = 'ACCEPTED_BY_GMAIL') AS accepted,
          count(m.id) FILTER (WHERE m.status IN ('PENDING','SCHEDULED','PROCESSING','RETRYABLE')) AS pending,
          count(m.id) FILTER (WHERE m.status = 'SKIPPED') AS skipped,
          count(m.id) FILTER (WHERE m.status = 'FAILED_PERMANENT') AS failed,
          count(m.id) FILTER (WHERE m.status = 'AMBIGUOUS') AS ambiguous,
          count(m.id) FILTER (WHERE m.status = 'CANCELLED') AS cancelled
        FROM campaign c
        LEFT JOIN message_record m ON m.organization_id = c.organization_id AND m.campaign_id = c.id
        WHERE c.organization_id = ? AND c.id = ?
        GROUP BY c.id, c.status, c.version, c.recipient_count, c.excluded_count
        """,
        (rs, ignored) ->
            new CampaignProgress(
                rs.getObject("id", UUID.class),
                CampaignState.valueOf(rs.getString("status")),
                rs.getLong("version"),
                rs.getInt("recipient_count"),
                rs.getInt("excluded_count"),
                rs.getInt("accepted"),
                rs.getInt("pending"),
                rs.getInt("skipped"),
                rs.getInt("failed"),
                rs.getInt("ambiguous"),
                rs.getInt("cancelled"),
                "Accepted by Gmail does not prove delivery to the mailbox"),
        currentActor.organizationId(),
        campaignId);
  }

  @Transactional(readOnly = true)
  public List<RecipientResult> results(UUID campaignId) {
    liveCampaign(campaignId);
    return jdbcTemplate.query(
        """
        SELECT m.id, m.status, m.result_category, m.attempt_count, m.next_attempt_at,
          m.accepted_at, m.external_message_id, m.last_http_status, m.last_error_summary,
          cc.normalized_value, p.id AS prospect_id
        FROM message_record m
        JOIN prospect p ON p.id = m.prospect_id AND p.organization_id = m.organization_id
        LEFT JOIN contact_channel cc ON cc.id = m.contact_channel_id
          AND cc.organization_id = m.organization_id
        WHERE m.organization_id = ? AND m.campaign_id = ?
        ORDER BY m.created_at, m.id
        """,
        (rs, ignored) ->
            new RecipientResult(
                rs.getObject("id", UUID.class),
                rs.getObject("prospect_id", UUID.class),
                mask(rs.getString("normalized_value")),
                rs.getString("status"),
                rs.getString("result_category"),
                rs.getInt("attempt_count"),
                instant(rs, "next_attempt_at"),
                instant(rs, "accepted_at"),
                rs.getString("external_message_id"),
                (Integer) rs.getObject("last_http_status"),
                rs.getString("last_error_summary"),
                "ACCEPTED_BY_GMAIL".equals(rs.getString("status"))
                    ? "Enviado y aceptado por Gmail; no existe evidencia de entrega al buzón."
                    : null),
        currentActor.organizationId(),
        campaignId);
  }

  private void enqueue(
      LiveCampaign campaign, Recipient recipient, Instant availableAt, String correlationId) {
    UUID messageId = UUID.randomUUID();
    UUID unsubscribeId = UUID.randomUUID();
    RenderedTemplate rendered =
        renderer.render(
            campaign.subject(),
            campaign.textBody(),
            campaign.htmlBody(),
            recipient.values(campaign.name()));
    IssuedToken token =
        unsubscribeTokens.issue(unsubscribeId, currentActor.organizationId(), messageId);
    String initialStatus = availableAt.isAfter(clock.instant()) ? "SCHEDULED" : "PENDING";
    String idempotencyKey = "campaign-live:" + campaign.id() + ":" + recipient.audienceId();
    int inserted =
        jdbcTemplate.update(
            """
            INSERT INTO message_record (
              id, version, organization_id, campaign_id, prospect_id, contact_id,
              contact_channel_id, created_by, channel, direction, status, subject,
              body_text, body_html, provider, idempotency_key, request_hash,
              sender_account_id, audience_recipient_id, next_attempt_at, correlation_id,
              created_at, updated_at
            ) VALUES (?, 0, ?, ?, ?, ?, ?, ?, 'EMAIL', 'OUTBOUND', ?, ?, ?, ?, 'GMAIL',
              ?, ?, ?, ?, ?, ?, now(), now())
            ON CONFLICT (organization_id, idempotency_key) DO NOTHING
            """,
            messageId,
            currentActor.organizationId(),
            campaign.id(),
            recipient.prospectId(),
            recipient.contactId(),
            recipient.contactChannelId(),
            currentActor.userIdOrNull(),
            initialStatus,
            rendered.subject(),
            rendered.textBody(),
            rendered.htmlBody(),
            idempotencyKey,
            OutboxPublisher.sha256(
                rendered.subject() + "\n" + rendered.textBody() + "\n" + rendered.htmlBody()),
            campaign.senderAccountId(),
            recipient.audienceId(),
            Timestamp.from(availableAt),
            correlationId);
    if (inserted == 0) {
      return;
    }
    jdbcTemplate.update(
        """
        INSERT INTO unsubscribe_token (
          id, organization_id, campaign_id, message_id, contact_channel_id,
          token_hash, key_id, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, now())
        """,
        unsubscribeId,
        currentActor.organizationId(),
        campaign.id(),
        messageId,
        recipient.contactChannelId(),
        token.tokenHash(),
        token.keyId());
    outboxPublisher.publish(
        new PublishCommand(
            currentActor.organizationId(),
            "CAMPAIGN_MESSAGE_SEND_V1",
            1,
            "MESSAGE",
            messageId,
            Map.of("messageId", messageId.toString()),
            "campaign-message-send:" + messageId,
            correlationId,
            currentActor.userIdOrNull(),
            campaign.maxAttempts(),
            availableAt));
  }

  private void requireConfirmation(String confirmation) {
    if (!LIVE_CONFIRMATION.equals(confirmation)) {
      throw new UnprocessableEntityException(
          "Live campaign confirmation must exactly match " + LIVE_CONFIRMATION);
    }
  }

  private void requireLiveFlags(LiveCampaign campaign) {
    if (!sendingProperties.enabled()
        || sendingProperties.dryRun()
        || sendingProperties.environmentKillSwitch()
        || sendingProperties.dailyLimit() <= 0
        || !messagingProperties.realNetworkAllowed()
        || !"GMAIL_LIVE".equalsIgnoreCase(messagingProperties.emailMode())) {
      throw new UnprocessableEntityException("Live sending is blocked by environment policy");
    }
    try {
      gmailProperties.requireLiveConfigured();
    } catch (IllegalArgumentException | IllegalStateException exception) {
      throw new UnprocessableEntityException("Gmail live configuration is incomplete");
    }
    GuardSnapshot guard =
        jdbcTemplate.queryForObject(
            """
            SELECT o.campaign_daily_limit,
              COALESCE(max(CASE WHEN s.setting_key = 'sending.enabled' THEN s.setting_value END), 'false') AS enabled,
              COALESCE(max(CASE WHEN s.setting_key = 'sending.dry-run' THEN s.setting_value END), 'true') AS dry_run,
              COALESCE(max(CASE WHEN s.setting_key = 'sending.daily-limit' THEN s.setting_value END), '0') AS daily_limit,
              COALESCE(max(CASE WHEN s.setting_key = 'sending.kill-switch' THEN s.setting_value END), 'true') AS kill_switch
            FROM organization o
            LEFT JOIN system_setting s ON s.organization_id = o.id
            WHERE o.id = ? AND o.active = TRUE
            GROUP BY o.campaign_daily_limit
            """,
            (rs, ignored) ->
                new GuardSnapshot(
                    rs.getInt("campaign_daily_limit"),
                    Boolean.parseBoolean(rs.getString("enabled")),
                    Boolean.parseBoolean(rs.getString("dry_run")),
                    Integer.parseInt(rs.getString("daily_limit")),
                    Boolean.parseBoolean(rs.getString("kill_switch"))),
            currentActor.organizationId());
    if (!guard.enabled()
        || guard.dryRun()
        || guard.killSwitch()
        || guard.dailyLimit() <= 0
        || guard.organizationLimit() <= 0) {
      throw new UnprocessableEntityException("Live sending is blocked by organization policy");
    }
    SenderSnapshot sender = sender(campaign.senderAccountId());
    if (!"CONNECTED".equals(sender.status())) {
      throw new UnprocessableEntityException("Sender account is not connected");
    }
    if (!sender.scopes().contains(GmailDeliveryProperties.SEND_SCOPE)) {
      throw new UnprocessableEntityException("Sender account lacks the gmail.send scope");
    }
    int effective =
        Math.min(
            Math.min(
                Math.min(sendingProperties.dailyLimit(), gmailProperties.hardDailyLimit()),
                guard.dailyLimit()),
            Math.min(guard.organizationLimit(), sender.dailyLimit()));
    if (campaign.dailyLimit() <= 0 || campaign.dailyLimit() > effective) {
      throw new UnprocessableEntityException(
          "Campaign daily limit exceeds the effective safety cap");
    }
  }

  private LiveCampaign liveCampaign(UUID campaignId) {
    return jdbcTemplate
        .query(
            """
            SELECT c.id, c.version, c.name, c.status, c.execution_mode, c.approved,
              c.approval_fingerprint, c.frozen_at, c.recipient_count, c.excluded_count,
              c.sender_account_id, c.daily_limit, c.max_attempts,
              tv.subject, tv.text_body, tv.html_body
            FROM campaign c
            JOIN template_version tv ON tv.id = c.template_version_id
              AND tv.organization_id = c.organization_id
            WHERE c.organization_id = ? AND c.id = ?
            """,
            (rs, rowNum) -> liveCampaignRow(rs, rowNum),
            currentActor.organizationId(),
            campaignId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
  }

  private LiveCampaign liveCampaignRow(ResultSet rs, int ignored) throws SQLException {
    return new LiveCampaign(
        rs.getObject("id", UUID.class),
        rs.getLong("version"),
        rs.getString("name"),
        CampaignState.valueOf(rs.getString("status")),
        CampaignExecutionMode.valueOf(rs.getString("execution_mode")),
        rs.getBoolean("approved"),
        rs.getString("approval_fingerprint"),
        instant(rs, "frozen_at"),
        rs.getInt("recipient_count"),
        rs.getInt("excluded_count"),
        rs.getObject("sender_account_id", UUID.class),
        rs.getInt("daily_limit"),
        rs.getInt("max_attempts"),
        rs.getString("subject"),
        rs.getString("text_body"),
        rs.getString("html_body"));
  }

  private List<Recipient> recipients(UUID campaignId) {
    return jdbcTemplate.query(
        """
        SELECT a.id AS audience_id, a.prospect_id, a.contact_id, a.contact_channel_id,
          i.name AS display_name, i.locality AS city, c.first_name, c.last_name,
          owner.display_name AS owner_name
        FROM campaign_audience_recipient a
        JOIN prospect p ON p.id = a.prospect_id AND p.organization_id = a.organization_id
        JOIN institution i ON i.id = p.institution_id AND i.organization_id = p.organization_id
        JOIN contact c ON c.id = a.contact_id AND c.organization_id = a.organization_id
        JOIN contact_channel cc ON cc.id = a.contact_channel_id AND cc.organization_id = a.organization_id
        LEFT JOIN app_user owner ON owner.id = p.owner_user_id
        WHERE a.organization_id = ? AND a.campaign_id = ? AND a.included = TRUE
        ORDER BY a.prospect_id
        """,
        (rs, ignored) ->
            new Recipient(
                rs.getObject("audience_id", UUID.class),
                rs.getObject("prospect_id", UUID.class),
                rs.getObject("contact_id", UUID.class),
                rs.getObject("contact_channel_id", UUID.class),
                rs.getString("display_name"),
                rs.getString("city"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("owner_name")),
        currentActor.organizationId(),
        campaignId);
  }

  private SenderSnapshot sender(UUID senderId) {
    return jdbcTemplate
        .query(
            """
            SELECT status, granted_scopes, daily_limit FROM integration_connection
            WHERE organization_id = ? AND id = ? AND provider = 'GMAIL'
            """,
            (rs, ignored) ->
                new SenderSnapshot(
                    rs.getString("status"),
                    Set.of((String[]) rs.getArray("granted_scopes").getArray()),
                    rs.getInt("daily_limit")),
            currentActor.organizationId(),
            senderId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Sender account not found: " + senderId));
  }

  private Instant instant(ResultSet rs, String column) throws SQLException {
    return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toInstant();
  }

  private String mask(String value) {
    if (value == null || !value.contains("@")) {
      return "oculto";
    }
    String[] parts = value.split("@", 2);
    return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
  }

  private record LiveCampaign(
      UUID id,
      long version,
      String name,
      CampaignState status,
      CampaignExecutionMode executionMode,
      boolean approved,
      String approvalFingerprint,
      Instant frozenAt,
      int recipientCount,
      int excludedCount,
      UUID senderAccountId,
      int dailyLimit,
      int maxAttempts,
      String subject,
      String textBody,
      String htmlBody) {}

  private record Recipient(
      UUID audienceId,
      UUID prospectId,
      UUID contactId,
      UUID contactChannelId,
      String displayName,
      String city,
      String firstName,
      String lastName,
      String ownerName) {
    Map<String, String> values(String campaignName) {
      Map<String, String> values = new LinkedHashMap<>();
      values.put("prospect.displayName", displayName);
      values.put("prospect.city", city == null ? "Sin localidad" : city);
      values.put("contact.firstName", firstName == null ? "Contacto" : firstName);
      values.put("contact.lastName", lastName == null || lastName.isBlank() ? "—" : lastName);
      values.put("owner.name", ownerName == null ? "Equipo Gestudio" : ownerName);
      values.put("campaign.name", campaignName);
      return values;
    }
  }

  private record GuardSnapshot(
      int organizationLimit, boolean enabled, boolean dryRun, int dailyLimit, boolean killSwitch) {}

  private record SenderSnapshot(String status, Set<String> scopes, int dailyLimit) {}

  public record CampaignProgress(
      UUID campaignId,
      CampaignState status,
      long version,
      int recipients,
      int excluded,
      int acceptedByGmail,
      int pending,
      int skipped,
      int failed,
      int ambiguous,
      int cancelled,
      String acceptanceDisclaimer) {}

  public record RecipientResult(
      UUID messageId,
      UUID prospectId,
      String maskedRecipient,
      String status,
      String resultCategory,
      int attempts,
      Instant nextAttemptAt,
      Instant acceptedAt,
      String providerMessageId,
      Integer httpStatus,
      String error,
      String acceptanceDisclaimer) {}
}
