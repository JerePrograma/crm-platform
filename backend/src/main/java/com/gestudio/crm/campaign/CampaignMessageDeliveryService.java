package com.gestudio.crm.campaign;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.gmail.GmailApiClient;
import com.gestudio.crm.gmail.GmailApiClient.SendResult;
import com.gestudio.crm.gmail.GmailDeliveryProperties;
import com.gestudio.crm.gmail.GmailMimeBuilder;
import com.gestudio.crm.gmail.GmailProblemException;
import com.gestudio.crm.gmail.GmailProviderException;
import com.gestudio.crm.gmail.GmailSenderAccountService;
import com.gestudio.crm.gmail.GoogleOAuthException;
import com.gestudio.crm.gmail.UnsubscribeTokenService;
import com.gestudio.crm.messaging.MessagingProperties;
import com.gestudio.crm.outbox.OutboxErrorCategory;
import com.gestudio.crm.outbox.OutboxProcessingResult;
import com.gestudio.crm.settings.SendingProperties;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CampaignMessageDeliveryService {

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final Set<String> TERMINAL =
      Set.of("ACCEPTED_BY_GMAIL", "AMBIGUOUS", "FAILED_PERMANENT", "CANCELLED", "SKIPPED");

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;
  private final SendingProperties sendingProperties;
  private final MessagingProperties messagingProperties;
  private final GmailDeliveryProperties gmailProperties;
  private final GmailSenderAccountService senderAccounts;
  private final GmailApiClient gmailApiClient;
  private final GmailMimeBuilder mimeBuilder;
  private final UnsubscribeTokenService unsubscribeTokens;
  private final AuditEventWriter auditEventWriter;
  private final Clock clock;

  public CampaignMessageDeliveryService(
      JdbcTemplate jdbcTemplate,
      TransactionTemplate transactionTemplate,
      SendingProperties sendingProperties,
      MessagingProperties messagingProperties,
      GmailDeliveryProperties gmailProperties,
      GmailSenderAccountService senderAccounts,
      GmailApiClient gmailApiClient,
      GmailMimeBuilder mimeBuilder,
      UnsubscribeTokenService unsubscribeTokens,
      AuditEventWriter auditEventWriter,
      Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.transactionTemplate = transactionTemplate;
    this.sendingProperties = sendingProperties;
    this.messagingProperties = messagingProperties;
    this.gmailProperties = gmailProperties;
    this.senderAccounts = senderAccounts;
    this.gmailApiClient = gmailApiClient;
    this.mimeBuilder = mimeBuilder;
    this.unsubscribeTokens = unsubscribeTokens;
    this.auditEventWriter = auditEventWriter;
    this.clock = clock;
  }

  public OutboxProcessingResult process(
      UUID organizationId,
      UUID messageId,
      int outboxAttempt,
      int outboxMaxAttempts,
      String correlationId) {
    Preparation preparation =
        transactionTemplate.execute(ignored -> prepare(organizationId, messageId, correlationId));
    if (preparation == null) {
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.RETRYABLE, "PREPARATION_FAILED", "Message preparation failed");
    }
    if (preparation.terminal() != null) {
      return preparation.terminal();
    }
    PreparedMessage message = preparation.message();
    String accessToken;
    try {
      accessToken =
          senderAccounts.accessTokenFor(organizationId, message.senderAccountId()).value();
    } catch (GmailProblemException exception) {
      return transactionTemplate.execute(
          ignored ->
              "GMAIL_REAUTH_REQUIRED".equals(exception.code())
                      || "GMAIL_ACCOUNT_NOT_CONNECTED".equals(exception.code())
                  ? tokenReauthRequired(message)
                  : retryBeforeTransmission(
                      message,
                      exception.code(),
                      "Gmail access token could not be obtained",
                      outboxAttempt,
                      outboxMaxAttempts));
    } catch (GoogleOAuthException exception) {
      return transactionTemplate.execute(
          ignored -> tokenFailure(message, exception, outboxAttempt, outboxMaxAttempts));
    } catch (RuntimeException exception) {
      return transactionTemplate.execute(
          ignored ->
              retryBeforeTransmission(
                  message,
                  "TOKEN_REFRESH_FAILED",
                  "Gmail access token could not be refreshed",
                  outboxAttempt,
                  outboxMaxAttempts));
    }

    GmailMimeBuilder.Message mime =
        new GmailMimeBuilder.Message(
            message.fromEmail(),
            message.fromDisplayName(),
            message.recipient(),
            message.replyTo(),
            message.subject(),
            message.textBody(),
            message.htmlBody(),
            message.unsubscribeUri(),
            message.id(),
            clock.instant());
    OutboxProcessingResult transmissionBlocked;
    try {
      transmissionBlocked = transactionTemplate.execute(ignored -> beginTransmission(message));
    } catch (RuntimeException exception) {
      return transactionTemplate.execute(
          ignored ->
              retryBeforeTransmission(
                  message,
                  "PRE_SEND_VALIDATION_FAILED",
                  "Final recipient validation could not be completed",
                  outboxAttempt,
                  outboxMaxAttempts));
    }
    if (transmissionBlocked != null) {
      return transmissionBlocked;
    }
    try {
      SendResult result = gmailApiClient.send(accessToken, mime);
      return transactionTemplate.execute(ignored -> accepted(message, result));
    } catch (GmailProviderException exception) {
      return transactionTemplate.execute(
          ignored -> providerFailure(message, exception, outboxAttempt, outboxMaxAttempts));
    } catch (RuntimeException exception) {
      return transactionTemplate.execute(
          ignored -> ambiguous(message, null, "Gmail result is unknown and requires review"));
    }
  }

  private Preparation prepare(UUID organizationId, UUID messageId, String correlationId) {
    DeliveryRow row = row(organizationId, messageId);
    if (row == null) {
      return terminal(
          OutboxProcessingResult.failure(
              OutboxErrorCategory.NON_RETRYABLE, "MESSAGE_NOT_FOUND", "Message not found"));
    }
    if (TERMINAL.contains(row.messageStatus())) {
      return terminal(
          OutboxProcessingResult.failure(
              "CANCELLED".equals(row.messageStatus())
                  ? OutboxErrorCategory.CANCELLED
                  : OutboxErrorCategory.DUPLICATE,
              "MESSAGE_TERMINAL",
              "Message already has a terminal result"));
    }
    if ("PROCESSING".equals(row.messageStatus()) && row.transmissionStartedAt() != null) {
      markAmbiguous(row, "Worker lease expired after transmission began");
      return terminal(
          OutboxProcessingResult.failure(
              OutboxErrorCategory.AMBIGUOUS,
              "LEASE_EXPIRED_AMBIGUOUS",
              "Previous Gmail result is unknown and requires review"));
    }
    advisoryLock(row.organizationId());
    advisoryLock(row.contactChannelId());
    advisoryLock(row.senderAccountId());
    row = row(organizationId, messageId);

    Instant now = clock.instant();
    if (row.campaignStatus() == CampaignState.PAUSED) {
      return deferred(row, "CAMPAIGN_PAUSED", "Campaign is paused", now.plus(Duration.ofHours(1)));
    }
    if (row.campaignStatus() == CampaignState.CANCELLED) {
      markTerminal(row, "CANCELLED", "CAMPAIGN_CANCELLED", "Campaign was cancelled");
      return terminal(
          OutboxProcessingResult.failure(
              OutboxErrorCategory.CANCELLED, "CAMPAIGN_CANCELLED", "Campaign was cancelled"));
    }
    if (row.campaignStatus() == CampaignState.SCHEDULED
        && row.scheduledAt() != null
        && row.scheduledAt().isAfter(now)) {
      return deferred(
          row, "CAMPAIGN_SCHEDULED", "Campaign is scheduled for later", row.scheduledAt());
    }
    if (row.campaignStatus() == CampaignState.SCHEDULED) {
      jdbcTemplate.update(
          """
          UPDATE campaign SET status = 'RUNNING', started_at = COALESCE(started_at, now()),
            updated_at = now(), version = version + 1
          WHERE organization_id = ? AND id = ? AND status = 'SCHEDULED'
          """,
          organizationId,
          row.campaignId());
      row = row.withCampaignStatus(CampaignState.RUNNING);
    }
    if (row.campaignStatus() != CampaignState.RUNNING
        || row.executionMode() != CampaignExecutionMode.LIVE
        || !row.approved()
        || row.approvalFingerprint() == null) {
      markTerminal(row, "SKIPPED", "CAMPAIGN_NOT_ACTIVE", "Campaign is not active and approved");
      return terminal(OutboxProcessingResult.success("RECIPIENT_SKIPPED"));
    }
    boolean suppressed = globalSuppressed(row.normalizedRecipient());
    if (!row.organizationActive()
        || !row.actorAuthorized()
        || !row.audienceIncluded()
        || !row.contactEligible()
        || row.contactDeleted()
        || !row.channelValid()
        || "DENIED".equals(row.contactConsent())
        || "DENIED".equals(row.channelConsent())
        || Set.of("CUSTOMER", "DO_NOT_CONTACT").contains(row.prospectStatus())
        || row.excluded()
        || suppressed) {
      String code = row.excluded() || suppressed ? "BLOCKED_BY_EXCLUSION" : "BLOCKED_BY_POLICY";
      markTerminal(row, "SKIPPED", code, "Recipient is no longer contactable");
      auditEventWriter.recordFor(
          organizationId,
          null,
          "CAMPAIGN_RECIPIENT_SKIPPED",
          "MESSAGE",
          row.id(),
          "SUCCESS",
          Map.of("reason", code, "campaignId", row.campaignId()));
      return terminal(OutboxProcessingResult.success("RECIPIENT_SKIPPED"));
    }
    if (row.normalizedRecipient() == null
        || row.normalizedRecipient().length() > 320
        || !EMAIL.matcher(row.normalizedRecipient()).matches()) {
      markTerminal(row, "FAILED_PERMANENT", "INVALID_RECIPIENT", "Recipient email is invalid");
      return terminal(
          OutboxProcessingResult.failure(
              OutboxErrorCategory.NON_RETRYABLE,
              "INVALID_RECIPIENT",
              "Recipient email is invalid"));
    }
    if (!"CONNECTED".equals(row.senderStatus())
        || !row.senderScopes().contains(GmailDeliveryProperties.SEND_SCOPE)) {
      pauseForReauth(row, "Sender account requires renewed authorization");
      return terminal(
          OutboxProcessingResult.failure(
              OutboxErrorCategory.CONFIGURATION_BLOCK,
              "SENDER_REAUTH_REQUIRED",
              "Sender account requires renewed authorization"));
    }
    Guard guard = guard(organizationId);
    if (guard.blocked()) {
      pause(row, "SENDING_GUARD_BLOCKED");
      return deferred(
          row,
          "SENDING_GUARD_BLOCKED",
          "Live sending is blocked by a safety guard",
          now.plus(Duration.ofHours(1)));
    }
    Instant allowedAt = nextAllowed(row, now);
    if (allowedAt.isAfter(now)) {
      return deferred(row, "OUTSIDE_OPERATING_WINDOW", "Outside operating window", allowedAt);
    }
    int effectiveLimit =
        Math.min(
            Math.min(
                Math.min(sendingProperties.dailyLimit(), gmailProperties.hardDailyLimit()),
                guard.databaseLimit()),
            Math.min(
                Math.min(guard.organizationLimit(), row.senderDailyLimit()),
                row.campaignDailyLimit()));
    if (effectiveLimit <= 0) {
      pause(row, "DAILY_LIMIT_ZERO");
      return deferred(
          row, "DAILY_LIMIT_ZERO", "Effective daily limit is zero", now.plus(Duration.ofHours(1)));
    }
    if (row.senderNextSendAt() != null && row.senderNextSendAt().isAfter(now)) {
      return deferred(
          row, "MINIMUM_INTERVAL", "Minimum sender interval is active", row.senderNextSendAt());
    }
    LocalDate localDate = now.atZone(ZoneId.of(row.timezone())).toLocalDate();
    if (!reserve(row, localDate, effectiveLimit, now)) {
      return deferred(
          row,
          "DAILY_LIMIT_REACHED",
          "Daily sending limit was reached",
          nextBusinessWindow(row, localDate.plusDays(1)));
    }
    int changed =
        jdbcTemplate.update(
            """
            UPDATE message_record SET status = 'PROCESSING', transmission_started_at = NULL,
              attempt_count = attempt_count + 1, next_attempt_at = NULL,
              correlation_id = ?, updated_at = now(), version = version + 1
            WHERE organization_id = ? AND id = ?
              AND status IN ('PENDING', 'SCHEDULED', 'RETRYABLE', 'PROCESSING')
            """,
            correlationId,
            organizationId,
            messageId);
    if (changed != 1) {
      release(row, false);
      return terminal(
          OutboxProcessingResult.failure(
              OutboxErrorCategory.DUPLICATE,
              "MESSAGE_ALREADY_CLAIMED",
              "Message was already claimed"));
    }
    var issued = unsubscribeTokens.issue(row.unsubscribeTokenId(), organizationId, row.id());
    return new Preparation(
        new PreparedMessage(
            row.id(),
            row.organizationId(),
            row.campaignId(),
            row.senderAccountId(),
            row.senderEmail(),
            row.senderDisplayName() == null ? row.senderEmail() : row.senderDisplayName(),
            row.normalizedRecipient(),
            row.replyTo(),
            row.subject(),
            row.bodyText(),
            row.bodyHtml(),
            issued.publicUri(),
            Math.max(row.senderMinIntervalSeconds(), row.campaignMinIntervalSeconds()),
            correlationId),
        null);
  }

  private OutboxProcessingResult beginTransmission(PreparedMessage message) {
    DeliveryRow row = row(message.organizationId(), message.id());
    if (row == null || !"PROCESSING".equals(row.messageStatus())) {
      settleLedger(message, false, true);
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.CANCELLED,
          "MESSAGE_NO_LONGER_SENDABLE",
          "Message is no longer eligible for transmission");
    }
    advisoryLock(row.organizationId());
    advisoryLock(row.contactChannelId());
    advisoryLock(row.senderAccountId());
    row = row(message.organizationId(), message.id());
    if (row == null || !"PROCESSING".equals(row.messageStatus())) {
      settleLedger(message, false, true);
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.CANCELLED,
          "MESSAGE_NO_LONGER_SENDABLE",
          "Message is no longer eligible for transmission");
    }
    Instant now = clock.instant();
    if (row.campaignStatus() == CampaignState.PAUSED) {
      release(row, false);
      return deferred(row, "CAMPAIGN_PAUSED", "Campaign is paused", now.plus(Duration.ofHours(1)))
          .terminal();
    }
    if (row.campaignStatus() == CampaignState.CANCELLED) {
      release(row, false);
      markTerminal(row, "CANCELLED", "CAMPAIGN_CANCELLED", "Campaign was cancelled");
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.CANCELLED, "CAMPAIGN_CANCELLED", "Campaign was cancelled");
    }
    if (row.campaignStatus() != CampaignState.RUNNING
        || row.executionMode() != CampaignExecutionMode.LIVE
        || !row.approved()
        || row.approvalFingerprint() == null) {
      release(row, false);
      markTerminal(row, "SKIPPED", "CAMPAIGN_NOT_ACTIVE", "Campaign is not active and approved");
      completeCampaign(row.organizationId(), row.campaignId());
      return OutboxProcessingResult.success("RECIPIENT_SKIPPED");
    }
    boolean suppressed = globalSuppressed(row.normalizedRecipient());
    if (!row.organizationActive()
        || !row.actorAuthorized()
        || !row.audienceIncluded()
        || !row.contactEligible()
        || row.contactDeleted()
        || !row.channelValid()
        || "DENIED".equals(row.contactConsent())
        || "DENIED".equals(row.channelConsent())
        || Set.of("CUSTOMER", "DO_NOT_CONTACT").contains(row.prospectStatus())
        || row.excluded()
        || suppressed) {
      String code = row.excluded() || suppressed ? "BLOCKED_BY_EXCLUSION" : "BLOCKED_BY_POLICY";
      release(row, false);
      markTerminal(row, "SKIPPED", code, "Recipient is no longer contactable");
      auditEventWriter.recordFor(
          row.organizationId(),
          null,
          "CAMPAIGN_RECIPIENT_SKIPPED",
          "MESSAGE",
          row.id(),
          "SUCCESS",
          Map.of("reason", code, "campaignId", row.campaignId()));
      completeCampaign(row.organizationId(), row.campaignId());
      return OutboxProcessingResult.success("RECIPIENT_SKIPPED");
    }
    if (!"CONNECTED".equals(row.senderStatus())
        || !row.senderScopes().contains(GmailDeliveryProperties.SEND_SCOPE)) {
      release(row, false);
      pauseForReauth(row, "Sender account requires renewed authorization");
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.CONFIGURATION_BLOCK,
          "SENDER_REAUTH_REQUIRED",
          "Sender account requires renewed authorization");
    }
    Guard guard = guard(row.organizationId());
    if (guard.blocked()) {
      release(row, false);
      pause(row, "SENDING_GUARD_BLOCKED");
      return deferred(
              row,
              "SENDING_GUARD_BLOCKED",
              "Live sending is blocked by a safety guard",
              now.plus(Duration.ofHours(1)))
          .terminal();
    }
    Instant allowedAt = nextAllowed(row, now);
    if (allowedAt.isAfter(now)) {
      release(row, false);
      return deferred(row, "OUTSIDE_OPERATING_WINDOW", "Outside operating window", allowedAt)
          .terminal();
    }
    int changed =
        jdbcTemplate.update(
            """
            UPDATE message_record AS m SET transmission_started_at = now(), updated_at = now(),
              version = version + 1
            WHERE m.organization_id = ? AND m.id = ? AND m.status = 'PROCESSING'
              AND m.transmission_started_at IS NULL
              AND EXISTS (
                SELECT 1 FROM campaign c
                WHERE c.id = m.campaign_id AND c.organization_id = m.organization_id
                  AND c.status = 'RUNNING' AND c.execution_mode = 'LIVE'
                  AND c.approved AND c.approval_fingerprint IS NOT NULL
              )
              AND NOT EXISTS (
                SELECT 1 FROM unsubscribe_token ut
                WHERE ut.organization_id = m.organization_id AND ut.message_id = m.id
                  AND ut.used_at IS NOT NULL
              )
              AND NOT EXISTS (
                SELECT 1 FROM contact_channel cc
                JOIN exclusion e ON e.organization_id = cc.organization_id
                  AND e.channel_type = 'EMAIL' AND e.normalized_value = cc.normalized_value
                WHERE cc.id = m.contact_channel_id
                  AND cc.organization_id = m.organization_id
              )
            """,
            message.organizationId(),
            message.id());
    if (changed == 1) {
      return null;
    }
    release(row, false);
    markTerminal(row, "SKIPPED", "BLOCKED_BY_POLICY", "Recipient is no longer contactable");
    completeCampaign(row.organizationId(), row.campaignId());
    return OutboxProcessingResult.success("RECIPIENT_SKIPPED");
  }

  private OutboxProcessingResult accepted(PreparedMessage message, SendResult result) {
    int changed =
        jdbcTemplate.update(
            """
            UPDATE message_record SET status = 'ACCEPTED_BY_GMAIL', external_message_id = ?,
              external_thread_id = ?, accepted_at = ?, last_http_status = ?,
              result_category = 'ACCEPTED', last_error_summary = NULL,
              transmission_started_at = NULL, updated_at = now(), version = version + 1
            WHERE organization_id = ? AND id = ? AND status = 'PROCESSING'
            """,
            result.providerMessageId(),
            result.threadId(),
            Timestamp.from(result.acceptedAt()),
            result.httpStatus(),
            message.organizationId(),
            message.id());
    if (changed != 1) {
      return ambiguous(
          message, result.httpStatus(), "Gmail accepted but local persistence conflicted");
    }
    settleLedger(message, true, false);
    jdbcTemplate.update(
        """
        UPDATE integration_connection SET next_send_at = ?, last_checked_at = now(),
          last_error_at = NULL, last_error_summary = NULL, updated_at = now(), version = version + 1
        WHERE organization_id = ? AND id = ? AND provider = 'GMAIL'
        """,
        Timestamp.from(result.acceptedAt().plusSeconds(message.minimumIntervalSeconds())),
        message.organizationId(),
        message.senderAccountId());
    attempt(
        message,
        "SUCCESS",
        result.httpStatus(),
        "ACCEPTED",
        null,
        null,
        result.providerMessageId());
    auditEventWriter.recordFor(
        message.organizationId(),
        null,
        "MESSAGE_ACCEPTED_BY_GMAIL",
        "MESSAGE",
        message.id(),
        "SUCCESS",
        Map.of(
            "campaignId", message.campaignId(),
            "providerMessageId", result.providerMessageId(),
            "httpStatus", result.httpStatus(),
            "deliveryProven", false));
    completeCampaign(message.organizationId(), message.campaignId());
    return OutboxProcessingResult.success("ACCEPTED_BY_GMAIL");
  }

  private OutboxProcessingResult providerFailure(
      PreparedMessage message,
      GmailProviderException exception,
      int outboxAttempt,
      int outboxMaxAttempts) {
    return switch (exception.category()) {
      case AMBIGUOUS, INVALID_RESPONSE ->
          ambiguous(message, exception.httpStatus(), "Gmail result is unknown and requires review");
      case REAUTH_REQUIRED, INSUFFICIENT_SCOPE -> {
        settleLedger(message, false, true);
        jdbcTemplate.update(
            """
            UPDATE integration_connection SET status = 'REAUTH_REQUIRED', last_error_at = now(),
              last_error_summary = 'Google authorization must be renewed', updated_at = now(),
              version = version + 1
            WHERE organization_id = ? AND id = ? AND provider = 'GMAIL' AND status <> 'REVOKED'
            """,
            message.organizationId(),
            message.senderAccountId());
        pauseMessage(message, "REAUTH_REQUIRED", "Google authorization must be renewed");
        attempt(
            message,
            "PERMANENT_FAILURE",
            exception.httpStatus(),
            "REAUTH_REQUIRED",
            null,
            "Google authorization must be renewed",
            null);
        yield OutboxProcessingResult.failure(
            OutboxErrorCategory.CONFIGURATION_BLOCK,
            "REAUTH_REQUIRED",
            "Google authorization must be renewed");
      }
      case RATE_LIMIT, QUOTA, RETRYABLE -> {
        Instant retryAt =
            exception.retryAt() == null ? clock.instant().plusSeconds(60) : exception.retryAt();
        if (outboxAttempt >= outboxMaxAttempts) {
          yield permanentFailure(
              message, exception.httpStatus(), "RETRY_EXHAUSTED", "Gmail retries were exhausted");
        }
        settleLedger(message, false, true);
        retryMessage(
            message,
            exception.httpStatus(),
            exception.category().name(),
            retryAt,
            exception.getMessage());
        yield OutboxProcessingResult.retry(
            exception.category().name(), exception.getMessage(), retryAt);
      }
      case VALIDATION, PERMANENT ->
          permanentFailure(
              message, exception.httpStatus(), exception.category().name(), exception.getMessage());
    };
  }

  private OutboxProcessingResult tokenFailure(
      PreparedMessage message,
      GoogleOAuthException exception,
      int outboxAttempt,
      int outboxMaxAttempts) {
    if (exception.code() == GoogleOAuthException.Code.INVALID_GRANT
        || exception.code() == GoogleOAuthException.Code.INSUFFICIENT_SCOPE) {
      settleLedger(message, false, true);
      jdbcTemplate.update(
          """
          UPDATE integration_connection SET status = 'REAUTH_REQUIRED', last_error_at = now(),
            last_error_summary = 'Google authorization must be renewed', updated_at = now(),
            version = version + 1
          WHERE organization_id = ? AND id = ? AND provider = 'GMAIL' AND status <> 'REVOKED'
          """,
          message.organizationId(),
          message.senderAccountId());
      pauseMessage(message, "REAUTH_REQUIRED", "Google authorization must be renewed");
      return OutboxProcessingResult.failure(
          OutboxErrorCategory.CONFIGURATION_BLOCK,
          "REAUTH_REQUIRED",
          "Google authorization must be renewed");
    }
    return retryBeforeTransmission(
        message,
        "TOKEN_REFRESH_FAILED",
        "Gmail access token could not be refreshed",
        outboxAttempt,
        outboxMaxAttempts);
  }

  private OutboxProcessingResult tokenReauthRequired(PreparedMessage message) {
    settleLedger(message, false, true);
    jdbcTemplate.update(
        """
        UPDATE integration_connection SET status = 'REAUTH_REQUIRED', last_error_at = now(),
          last_error_summary = 'Google authorization must be renewed', updated_at = now(),
          version = version + 1
        WHERE organization_id = ? AND id = ? AND provider = 'GMAIL' AND status <> 'REVOKED'
        """,
        message.organizationId(),
        message.senderAccountId());
    pauseMessage(message, "REAUTH_REQUIRED", "Google authorization must be renewed");
    return OutboxProcessingResult.failure(
        OutboxErrorCategory.CONFIGURATION_BLOCK,
        "REAUTH_REQUIRED",
        "Google authorization must be renewed");
  }

  private OutboxProcessingResult retryBeforeTransmission(
      PreparedMessage message,
      String code,
      String summary,
      int outboxAttempt,
      int outboxMaxAttempts) {
    if (outboxAttempt >= outboxMaxAttempts) {
      return permanentFailure(message, null, "RETRY_EXHAUSTED", "Gmail retries were exhausted");
    }
    settleLedger(message, false, true);
    Instant retryAt = clock.instant().plusSeconds(60);
    retryMessage(message, null, code, retryAt, summary);
    return OutboxProcessingResult.retry(code, summary, retryAt);
  }

  private OutboxProcessingResult permanentFailure(
      PreparedMessage message, Integer httpStatus, String category, String summary) {
    settleLedger(message, false, true);
    jdbcTemplate.update(
        """
        UPDATE message_record SET status = 'FAILED_PERMANENT', result_category = ?,
          last_http_status = ?, last_error_summary = ?, transmission_started_at = NULL,
          updated_at = now(), version = version + 1
        WHERE organization_id = ? AND id = ? AND status = 'PROCESSING'
        """,
        category,
        httpStatus,
        safe(summary),
        message.organizationId(),
        message.id());
    attempt(message, "PERMANENT_FAILURE", httpStatus, category, null, summary, null);
    auditEventWriter.recordFor(
        message.organizationId(),
        null,
        "MESSAGE_FAILED_PERMANENTLY",
        "MESSAGE",
        message.id(),
        "FAILED",
        Map.of("campaignId", message.campaignId(), "category", category));
    completeCampaign(message.organizationId(), message.campaignId());
    return OutboxProcessingResult.failure(OutboxErrorCategory.NON_RETRYABLE, category, summary);
  }

  private OutboxProcessingResult ambiguous(
      PreparedMessage message, Integer httpStatus, String summary) {
    settleLedger(message, false, false);
    jdbcTemplate.update(
        """
        UPDATE message_record SET status = 'AMBIGUOUS', result_category = 'AMBIGUOUS',
          last_http_status = ?, last_error_summary = ?, transmission_started_at = NULL,
          updated_at = now(), version = version + 1
        WHERE organization_id = ? AND id = ? AND status = 'PROCESSING'
        """,
        httpStatus,
        safe(summary),
        message.organizationId(),
        message.id());
    jdbcTemplate.update(
        """
        UPDATE campaign SET status = 'PAUSED', paused_at = now(), updated_at = now(),
          version = version + 1 WHERE organization_id = ? AND id = ? AND status = 'RUNNING'
        """,
        message.organizationId(),
        message.campaignId());
    attempt(message, "AMBIGUOUS", httpStatus, "AMBIGUOUS", null, summary, null);
    auditEventWriter.recordFor(
        message.organizationId(),
        null,
        "MESSAGE_RESULT_AMBIGUOUS",
        "MESSAGE",
        message.id(),
        "FAILED",
        Map.of("campaignId", message.campaignId(), "automaticRetry", false));
    return OutboxProcessingResult.failure(
        OutboxErrorCategory.AMBIGUOUS,
        "AMBIGUOUS_RESULT",
        "Gmail result is unknown and requires review");
  }

  private void retryMessage(
      PreparedMessage message,
      Integer httpStatus,
      String category,
      Instant retryAt,
      String summary) {
    jdbcTemplate.update(
        """
        UPDATE message_record SET status = 'RETRYABLE', result_category = ?,
          last_http_status = ?, last_error_summary = ?, next_attempt_at = ?,
          transmission_started_at = NULL, updated_at = now(), version = version + 1
        WHERE organization_id = ? AND id = ? AND status = 'PROCESSING'
        """,
        category,
        httpStatus,
        safe(summary),
        Timestamp.from(retryAt),
        message.organizationId(),
        message.id());
    attempt(message, "TRANSIENT_FAILURE", httpStatus, category, retryAt, summary, null);
    auditEventWriter.recordFor(
        message.organizationId(),
        null,
        "MESSAGE_RETRY_SCHEDULED",
        "MESSAGE",
        message.id(),
        "SUCCESS",
        Map.of("campaignId", message.campaignId(), "category", category, "retryAt", retryAt));
  }

  private void attempt(
      PreparedMessage message,
      String result,
      Integer httpStatus,
      String category,
      Instant retryAt,
      String summary,
      String externalId) {
    jdbcTemplate.update(
        """
        INSERT INTO message_provider_attempt (
          id, organization_id, message_id, provider, operation, result, http_status,
          external_id, started_at, completed_at, retry_after, result_category,
          correlation_id, response_summary
        ) VALUES (?, ?, ?, 'GMAIL', 'SEND', ?, ?, ?, now(), now(), ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        message.organizationId(),
        message.id(),
        result,
        httpStatus,
        externalId,
        retryAt == null ? null : Timestamp.from(retryAt),
        category,
        message.correlationId(),
        safe(summary));
  }

  private boolean reserve(DeliveryRow row, LocalDate localDate, int limit, Instant now) {
    List<Scope> scopes =
        List.of(
            new Scope("ORGANIZATION", row.organizationId()),
            new Scope("SENDER", row.senderAccountId()),
            new Scope("CAMPAIGN", row.campaignId()));
    for (Scope scope : scopes) {
      jdbcTemplate.update(
          """
          INSERT INTO delivery_daily_ledger (
            id, version, organization_id, scope_type, scope_id, local_date,
            reserved_count, accepted_count, released_count, created_at, updated_at
          ) VALUES (?, 0, ?, ?, ?, ?, 0, 0, 0, now(), now())
          ON CONFLICT (organization_id, scope_type, scope_id, local_date) DO NOTHING
          """,
          UUID.randomUUID(),
          row.organizationId(),
          scope.type(),
          scope.id(),
          localDate);
      Ledger ledger =
          jdbcTemplate.queryForObject(
              """
              SELECT reserved_count, released_count, lease_message_id, lease_expires_at
              FROM delivery_daily_ledger
              WHERE organization_id = ? AND scope_type = ? AND scope_id = ? AND local_date = ?
              FOR UPDATE
              """,
              (rs, ignored) ->
                  new Ledger(
                      rs.getInt("reserved_count"),
                      rs.getInt("released_count"),
                      rs.getObject("lease_message_id", UUID.class),
                      instant(rs, "lease_expires_at")),
              row.organizationId(),
              scope.type(),
              scope.id(),
              localDate);
      if (ledger.reserved() - ledger.released() >= limit
          || (ledger.leaseMessageId() != null
              && !ledger.leaseMessageId().equals(row.id())
              && ledger.leaseExpiresAt() != null
              && ledger.leaseExpiresAt().isAfter(now))) {
        return false;
      }
    }
    for (Scope scope : scopes) {
      jdbcTemplate.update(
          """
          UPDATE delivery_daily_ledger SET reserved_count = reserved_count + 1,
            lease_message_id = ?, lease_expires_at = ?, updated_at = now(), version = version + 1
          WHERE organization_id = ? AND scope_type = ? AND scope_id = ? AND local_date = ?
          """,
          row.id(),
          Timestamp.from(now.plusSeconds(300)),
          row.organizationId(),
          scope.type(),
          scope.id(),
          localDate);
    }
    return true;
  }

  private void settleLedger(PreparedMessage message, boolean accepted, boolean released) {
    jdbcTemplate.update(
        """
        UPDATE delivery_daily_ledger SET
          accepted_count = accepted_count + CASE WHEN ? THEN 1 ELSE 0 END,
          released_count = released_count + CASE WHEN ? THEN 1 ELSE 0 END,
          last_send_at = CASE WHEN ? THEN now() ELSE last_send_at END,
          lease_message_id = NULL, lease_expires_at = NULL, updated_at = now(), version = version + 1
        WHERE organization_id = ? AND lease_message_id = ?
        """,
        accepted,
        released,
        accepted,
        message.organizationId(),
        message.id());
  }

  private void release(DeliveryRow row, boolean accepted) {
    jdbcTemplate.update(
        """
        UPDATE delivery_daily_ledger SET
          accepted_count = accepted_count + CASE WHEN ? THEN 1 ELSE 0 END,
          released_count = released_count + CASE WHEN ? THEN 0 ELSE 1 END,
          lease_message_id = NULL, lease_expires_at = NULL, updated_at = now(), version = version + 1
        WHERE organization_id = ? AND lease_message_id = ?
        """,
        accepted,
        accepted,
        row.organizationId(),
        row.id());
  }

  private Guard guard(UUID organizationId) {
    Guard database =
        jdbcTemplate.queryForObject(
            """
            SELECT o.campaign_daily_limit,
              COALESCE(max(CASE WHEN s.setting_key = 'sending.enabled' THEN s.setting_value END), 'false') AS enabled,
              COALESCE(max(CASE WHEN s.setting_key = 'sending.dry-run' THEN s.setting_value END), 'true') AS dry_run,
              COALESCE(max(CASE WHEN s.setting_key = 'sending.daily-limit' THEN s.setting_value END), '0') AS daily_limit,
              COALESCE(max(CASE WHEN s.setting_key = 'sending.kill-switch' THEN s.setting_value END), 'true') AS kill_switch,
              COALESCE(max(CASE WHEN s.setting_key = 'outbox-worker-paused' THEN s.setting_value END), 'false') AS worker_paused
            FROM organization o LEFT JOIN system_setting s ON s.organization_id = o.id
            WHERE o.id = ? GROUP BY o.campaign_daily_limit
            """,
            (rs, ignored) ->
                new Guard(
                    rs.getInt("campaign_daily_limit"),
                    Boolean.parseBoolean(rs.getString("enabled")),
                    Boolean.parseBoolean(rs.getString("dry_run")),
                    Integer.parseInt(rs.getString("daily_limit")),
                    Boolean.parseBoolean(rs.getString("kill_switch")),
                    Boolean.parseBoolean(rs.getString("worker_paused")),
                    false),
            organizationId);
    boolean environmentBlocked =
        !sendingProperties.enabled()
            || sendingProperties.dryRun()
            || sendingProperties.environmentKillSwitch()
            || sendingProperties.dailyLimit() <= 0
            || !messagingProperties.realNetworkAllowed()
            || !"GMAIL_LIVE".equalsIgnoreCase(messagingProperties.emailMode());
    try {
      gmailProperties.requireLiveConfigured();
    } catch (IllegalArgumentException | IllegalStateException exception) {
      environmentBlocked = true;
    }
    return database.withEnvironmentBlocked(environmentBlocked);
  }

  private Instant nextAllowed(DeliveryRow row, Instant now) {
    ZonedDateTime local = now.atZone(ZoneId.of(row.timezone()));
    int day = local.getDayOfWeek().getValue();
    LocalTime time = local.toLocalTime();
    if (row.businessDays().contains(day)
        && !time.isBefore(row.windowStart())
        && time.isBefore(row.windowEnd())) {
      return now;
    }
    LocalDate date = local.toLocalDate();
    if (row.businessDays().contains(day) && time.isBefore(row.windowStart())) {
      return LocalDateTime.of(date, row.windowStart()).atZone(local.getZone()).toInstant();
    }
    return nextBusinessWindow(row, date.plusDays(1));
  }

  private Instant nextBusinessWindow(DeliveryRow row, LocalDate date) {
    ZoneId zone = ZoneId.of(row.timezone());
    for (int offset = 0; offset < 8; offset++) {
      LocalDate candidate = date.plusDays(offset);
      if (row.businessDays().contains(candidate.getDayOfWeek().getValue())) {
        return LocalDateTime.of(candidate, row.windowStart()).atZone(zone).toInstant();
      }
    }
    throw new IllegalStateException("Campaign business days are invalid");
  }

  private boolean globalSuppressed(String normalizedValue) {
    if (normalizedValue == null) {
      return false;
    }
    return Boolean.TRUE.equals(
        jdbcTemplate.query(
            "SELECT EXISTS (SELECT 1 FROM global_contact_suppression WHERE channel_type = 'EMAIL' AND value_hash = ?)",
            rs -> rs.next() && rs.getBoolean(1),
            unsubscribeTokens.suppressionHash(normalizedValue)));
  }

  private Preparation deferred(DeliveryRow row, String code, String summary, Instant retryAt) {
    jdbcTemplate.update(
        """
        UPDATE message_record SET status = 'RETRYABLE', result_category = ?,
          last_error_summary = ?, next_attempt_at = ?, transmission_started_at = NULL,
          updated_at = now(), version = version + 1
        WHERE organization_id = ? AND id = ?
          AND status IN ('PENDING','SCHEDULED','RETRYABLE','PROCESSING')
        """,
        code,
        summary,
        Timestamp.from(retryAt),
        row.organizationId(),
        row.id());
    return terminal(OutboxProcessingResult.defer(code, summary, retryAt));
  }

  private void markTerminal(DeliveryRow row, String status, String category, String summary) {
    jdbcTemplate.update(
        """
        UPDATE message_record SET status = ?, result_category = ?, last_error_summary = ?,
          next_attempt_at = NULL, transmission_started_at = NULL, updated_at = now(),
          version = version + 1 WHERE organization_id = ? AND id = ?
        """,
        status,
        category,
        summary,
        row.organizationId(),
        row.id());
  }

  private void markAmbiguous(DeliveryRow row, String summary) {
    markTerminal(row, "AMBIGUOUS", "AMBIGUOUS", summary);
    pause(row, "AMBIGUOUS_RESULT");
    auditEventWriter.recordFor(
        row.organizationId(),
        null,
        "MESSAGE_RESULT_AMBIGUOUS",
        "MESSAGE",
        row.id(),
        "FAILED",
        Map.of("campaignId", row.campaignId(), "automaticRetry", false));
  }

  private void pauseForReauth(DeliveryRow row, String summary) {
    jdbcTemplate.update(
        """
        UPDATE integration_connection SET status = 'REAUTH_REQUIRED', last_error_at = now(),
          last_error_summary = ?, updated_at = now(), version = version + 1
        WHERE organization_id = ? AND id = ? AND status <> 'REVOKED'
        """,
        summary,
        row.organizationId(),
        row.senderAccountId());
    deferred(row, "REAUTH_REQUIRED", summary, clock.instant().plus(Duration.ofHours(1)));
    pause(row, "REAUTH_REQUIRED");
  }

  private void pause(DeliveryRow row, String reason) {
    jdbcTemplate.update(
        """
        UPDATE campaign SET status = 'PAUSED', paused_at = now(), failure_message = ?,
          updated_at = now(), version = version + 1
        WHERE organization_id = ? AND id = ? AND status IN ('RUNNING','SCHEDULED')
        """,
        safe(reason),
        row.organizationId(),
        row.campaignId());
  }

  private void pauseMessage(PreparedMessage message, String code, String summary) {
    jdbcTemplate.update(
        """
        UPDATE message_record SET status = 'RETRYABLE', result_category = ?,
          last_error_summary = ?, next_attempt_at = ?, transmission_started_at = NULL,
          updated_at = now(), version = version + 1
        WHERE organization_id = ? AND id = ? AND status = 'PROCESSING'
        """,
        code,
        summary,
        Timestamp.from(clock.instant().plus(Duration.ofHours(1))),
        message.organizationId(),
        message.id());
    jdbcTemplate.update(
        """
        UPDATE campaign SET status = 'PAUSED', paused_at = now(), failure_message = ?,
          updated_at = now(), version = version + 1
        WHERE organization_id = ? AND id = ? AND status = 'RUNNING'
        """,
        summary,
        message.organizationId(),
        message.campaignId());
  }

  private void completeCampaign(UUID organizationId, UUID campaignId) {
    int pending =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM message_record WHERE organization_id = ? AND campaign_id = ?
              AND status IN ('PENDING','SCHEDULED','PROCESSING','RETRYABLE')
            """,
            Integer.class,
            organizationId,
            campaignId);
    if (pending == 0) {
      int accepted =
          jdbcTemplate.queryForObject(
              "SELECT count(*) FROM message_record WHERE organization_id = ? AND campaign_id = ? AND status = 'ACCEPTED_BY_GMAIL'",
              Integer.class,
              organizationId,
              campaignId);
      CampaignState target = accepted > 0 ? CampaignState.COMPLETED : CampaignState.FAILED;
      jdbcTemplate.update(
          """
          UPDATE campaign SET status = ?, completed_at = CASE WHEN ? = 'COMPLETED' THEN now() ELSE completed_at END,
            updated_at = now(), version = version + 1
          WHERE organization_id = ? AND id = ? AND status = 'RUNNING'
          """,
          target.name(),
          target.name(),
          organizationId,
          campaignId);
      auditEventWriter.recordFor(
          organizationId,
          null,
          target == CampaignState.COMPLETED ? "CAMPAIGN_LIVE_COMPLETED" : "CAMPAIGN_LIVE_FAILED",
          "CAMPAIGN",
          campaignId,
          target == CampaignState.COMPLETED ? "SUCCESS" : "FAILED",
          Map.of("acceptedByGmail", accepted));
    }
  }

  private DeliveryRow row(UUID organizationId, UUID messageId) {
    return jdbcTemplate
        .query(
            """
            SELECT m.id, m.organization_id, m.campaign_id, m.status AS message_status,
              m.sender_account_id, m.contact_channel_id, m.subject, m.body_text, m.body_html,
              m.transmission_started_at, c.status AS campaign_status, c.execution_mode,
              c.approved, c.approval_fingerprint, c.scheduled_at, c.timezone,
              c.operating_window_start, c.operating_window_end, c.business_days,
              c.daily_limit AS campaign_daily_limit,
              c.minimum_interval_seconds AS campaign_min_interval_seconds,
              sender.status AS sender_status, sender.email_address AS sender_email,
              sender.display_name AS sender_display_name, sender.granted_scopes,
              sender.daily_limit AS sender_daily_limit,
              sender.min_interval_seconds AS sender_min_interval_seconds,
              sender.next_send_at AS sender_next_send_at, c.reply_to,
              p.status AS prospect_status, p.contact_eligible,
              contact.deleted_at IS NOT NULL AS contact_deleted,
              contact.consent AS contact_consent, cc.consent AS channel_consent,
              cc.valid AS channel_valid, cc.normalized_value,
              audience.included AS audience_included,
              organization.active AS organization_active,
              token.id AS unsubscribe_token_id,
              EXISTS (
                SELECT 1 FROM exclusion e WHERE e.organization_id = m.organization_id
                  AND e.channel_type = 'EMAIL' AND e.normalized_value = cc.normalized_value
              ) AS excluded,
              EXISTS (
                SELECT 1 FROM organization_membership om
                JOIN role_permission rp ON rp.role_id = om.role_id
                WHERE om.organization_id = m.organization_id AND om.user_id = m.created_by
                  AND om.active = TRUE AND rp.permission_code = 'MESSAGE_SEND'
              ) AS actor_authorized
            FROM message_record m
            JOIN campaign c ON c.id = m.campaign_id AND c.organization_id = m.organization_id
            JOIN integration_connection sender ON sender.id = m.sender_account_id
              AND sender.organization_id = m.organization_id AND sender.provider = 'GMAIL'
            JOIN prospect p ON p.id = m.prospect_id AND p.organization_id = m.organization_id
            JOIN contact contact ON contact.id = m.contact_id AND contact.organization_id = m.organization_id
            JOIN contact_channel cc ON cc.id = m.contact_channel_id AND cc.organization_id = m.organization_id
            JOIN campaign_audience_recipient audience ON audience.id = m.audience_recipient_id
              AND audience.organization_id = m.organization_id AND audience.campaign_id = m.campaign_id
              AND audience.contact_channel_id = m.contact_channel_id
            JOIN organization organization ON organization.id = m.organization_id
            JOIN unsubscribe_token token ON token.message_id = m.id AND token.organization_id = m.organization_id
            WHERE m.organization_id = ? AND m.id = ? AND m.provider = 'GMAIL'
            FOR UPDATE OF m, c, sender
            """,
            this::deliveryRow,
            organizationId,
            messageId)
        .stream()
        .findFirst()
        .orElse(null);
  }

  private DeliveryRow deliveryRow(ResultSet rs, int ignored) throws SQLException {
    return new DeliveryRow(
        rs.getObject("id", UUID.class),
        rs.getObject("organization_id", UUID.class),
        rs.getObject("campaign_id", UUID.class),
        rs.getString("message_status"),
        rs.getObject("sender_account_id", UUID.class),
        rs.getObject("contact_channel_id", UUID.class),
        rs.getString("subject"),
        rs.getString("body_text"),
        rs.getString("body_html"),
        instant(rs, "transmission_started_at"),
        CampaignState.valueOf(rs.getString("campaign_status")),
        CampaignExecutionMode.valueOf(rs.getString("execution_mode")),
        rs.getBoolean("approved"),
        rs.getString("approval_fingerprint"),
        instant(rs, "scheduled_at"),
        rs.getString("timezone"),
        rs.getTime("operating_window_start").toLocalTime(),
        rs.getTime("operating_window_end").toLocalTime(),
        days(rs.getArray("business_days")),
        rs.getInt("campaign_daily_limit"),
        rs.getInt("campaign_min_interval_seconds"),
        rs.getString("sender_status"),
        rs.getString("sender_email"),
        rs.getString("sender_display_name"),
        Set.copyOf(Arrays.asList((String[]) rs.getArray("granted_scopes").getArray())),
        rs.getInt("sender_daily_limit"),
        rs.getInt("sender_min_interval_seconds"),
        instant(rs, "sender_next_send_at"),
        rs.getString("reply_to"),
        rs.getString("prospect_status"),
        rs.getBoolean("contact_eligible"),
        rs.getBoolean("contact_deleted"),
        rs.getString("contact_consent"),
        rs.getString("channel_consent"),
        rs.getBoolean("channel_valid"),
        rs.getString("normalized_value"),
        rs.getBoolean("audience_included"),
        rs.getBoolean("organization_active"),
        rs.getObject("unsubscribe_token_id", UUID.class),
        rs.getBoolean("excluded"),
        rs.getBoolean("actor_authorized"));
  }

  private Set<Integer> days(Array array) throws SQLException {
    Number[] values = (Number[]) array.getArray();
    return Arrays.stream(values).map(Number::intValue).collect(java.util.stream.Collectors.toSet());
  }

  private void advisoryLock(UUID id) {
    jdbcTemplate.query(
        "SELECT pg_advisory_xact_lock(hashtextextended(?::text, 0))",
        resultSet -> null,
        id.toString());
  }

  private Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private String safe(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
    return normalized.substring(0, Math.min(normalized.length(), 500));
  }

  private Preparation terminal(OutboxProcessingResult result) {
    return new Preparation(null, result);
  }

  private record Preparation(PreparedMessage message, OutboxProcessingResult terminal) {}

  private record PreparedMessage(
      UUID id,
      UUID organizationId,
      UUID campaignId,
      UUID senderAccountId,
      String fromEmail,
      String fromDisplayName,
      String recipient,
      String replyTo,
      String subject,
      String textBody,
      String htmlBody,
      java.net.URI unsubscribeUri,
      int minimumIntervalSeconds,
      String correlationId) {}

  private record Scope(String type, UUID id) {}

  private record Ledger(int reserved, int released, UUID leaseMessageId, Instant leaseExpiresAt) {}

  private record Guard(
      int organizationLimit,
      boolean databaseEnabled,
      boolean databaseDryRun,
      int databaseLimit,
      boolean databaseKillSwitch,
      boolean workerPaused,
      boolean environmentBlocked) {
    boolean blocked() {
      return environmentBlocked
          || !databaseEnabled
          || databaseDryRun
          || databaseLimit <= 0
          || databaseKillSwitch
          || workerPaused
          || organizationLimit <= 0;
    }

    Guard withEnvironmentBlocked(boolean blocked) {
      return new Guard(
          organizationLimit,
          databaseEnabled,
          databaseDryRun,
          databaseLimit,
          databaseKillSwitch,
          workerPaused,
          blocked);
    }
  }

  private record DeliveryRow(
      UUID id,
      UUID organizationId,
      UUID campaignId,
      String messageStatus,
      UUID senderAccountId,
      UUID contactChannelId,
      String subject,
      String bodyText,
      String bodyHtml,
      Instant transmissionStartedAt,
      CampaignState campaignStatus,
      CampaignExecutionMode executionMode,
      boolean approved,
      String approvalFingerprint,
      Instant scheduledAt,
      String timezone,
      LocalTime windowStart,
      LocalTime windowEnd,
      Set<Integer> businessDays,
      int campaignDailyLimit,
      int campaignMinIntervalSeconds,
      String senderStatus,
      String senderEmail,
      String senderDisplayName,
      Set<String> senderScopes,
      int senderDailyLimit,
      int senderMinIntervalSeconds,
      Instant senderNextSendAt,
      String replyTo,
      String prospectStatus,
      boolean contactEligible,
      boolean contactDeleted,
      String contactConsent,
      String channelConsent,
      boolean channelValid,
      String normalizedRecipient,
      boolean audienceIncluded,
      boolean organizationActive,
      UUID unsubscribeTokenId,
      boolean excluded,
      boolean actorAuthorized) {
    DeliveryRow withCampaignStatus(CampaignState state) {
      return new DeliveryRow(
          id,
          organizationId,
          campaignId,
          messageStatus,
          senderAccountId,
          contactChannelId,
          subject,
          bodyText,
          bodyHtml,
          transmissionStartedAt,
          state,
          executionMode,
          approved,
          approvalFingerprint,
          scheduledAt,
          timezone,
          windowStart,
          windowEnd,
          businessDays,
          campaignDailyLimit,
          campaignMinIntervalSeconds,
          senderStatus,
          senderEmail,
          senderDisplayName,
          senderScopes,
          senderDailyLimit,
          senderMinIntervalSeconds,
          senderNextSendAt,
          replyTo,
          prospectStatus,
          contactEligible,
          contactDeleted,
          contactConsent,
          channelConsent,
          channelValid,
          normalizedRecipient,
          audienceIncluded,
          organizationActive,
          unsubscribeTokenId,
          excluded,
          actorAuthorized);
    }
  }
}
