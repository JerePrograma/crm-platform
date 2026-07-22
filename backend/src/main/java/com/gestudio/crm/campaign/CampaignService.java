package com.gestudio.crm.campaign;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.campaign.SafeTemplateRenderer.RenderedTemplate;
import com.gestudio.crm.common.CorrelationIds;
import com.gestudio.crm.common.OptimisticConflictException;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.common.UnprocessableEntityException;
import com.gestudio.crm.outbox.OutboxPublisher;
import com.gestudio.crm.outbox.OutboxPublisher.PublishCommand;
import com.gestudio.crm.security.CurrentActor;
import com.gestudio.crm.settings.SendingProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class CampaignService {

  private static final int MAX_AUDIENCE = 10_000;

  private final JdbcTemplate jdbcTemplate;
  private final CurrentActor currentActor;
  private final AuditEventWriter auditEventWriter;
  private final SafeTemplateRenderer renderer;
  private final SendingProperties sendingProperties;
  private final ObjectMapper objectMapper;
  private final OutboxPublisher outboxPublisher;

  public CampaignService(
      JdbcTemplate jdbcTemplate,
      CurrentActor currentActor,
      AuditEventWriter auditEventWriter,
      SafeTemplateRenderer renderer,
      SendingProperties sendingProperties,
      ObjectMapper objectMapper,
      OutboxPublisher outboxPublisher) {
    this.jdbcTemplate = jdbcTemplate;
    this.currentActor = currentActor;
    this.auditEventWriter = auditEventWriter;
    this.renderer = renderer;
    this.sendingProperties = sendingProperties;
    this.objectMapper = objectMapper;
    this.outboxPublisher = outboxPublisher;
  }

  @Transactional(readOnly = true)
  public List<TemplateView> templates() {
    return jdbcTemplate.query(
        """
        SELECT t.id, t.name, t.channel, t.archived_at,
          v.id AS version_id, v.version_number, v.subject, v.text_body, v.html_body,
          v.variables::text AS variables, v.created_at
        FROM email_template t
        LEFT JOIN LATERAL (
          SELECT * FROM template_version candidate
          WHERE candidate.organization_id = t.organization_id AND candidate.template_id = t.id
            AND candidate.archived_at IS NULL
          ORDER BY candidate.version_number DESC LIMIT 1
        ) v ON TRUE
        WHERE t.organization_id = ? AND t.archived_at IS NULL
        ORDER BY lower(t.name), t.id
        """,
        this::templateView,
        currentActor.organizationId());
  }

  @Transactional
  public TemplateView createTemplate(CreateTemplateCommand command) {
    String name = required(command.name(), "Template name");
    validateTemplate(command.subject(), command.textBody(), command.htmlBody());
    UUID templateId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO email_template (
          id, version, organization_id, name, channel, created_at, updated_at
        ) VALUES (?, 0, ?, ?, ?, now(), now())
        """,
        templateId,
        currentActor.organizationId(),
        name,
        command.channel().name());
    insertVersion(templateId, 1, command.subject(), command.textBody(), command.htmlBody());
    auditEventWriter.record(
        "TEMPLATE_CREATED", "EMAIL_TEMPLATE", templateId, Map.of("channel", command.channel()));
    return template(templateId);
  }

  @Transactional
  public TemplateView addTemplateVersion(UUID templateId, TemplateContent command) {
    TemplateView before = template(templateId);
    validateTemplate(command.subject(), command.textBody(), command.htmlBody());
    int nextVersion = before.versionNumber() + 1;
    insertVersion(
        templateId, nextVersion, command.subject(), command.textBody(), command.htmlBody());
    jdbcTemplate.update(
        "UPDATE email_template SET version = version + 1, updated_at = now() WHERE id = ? AND organization_id = ?",
        templateId,
        currentActor.organizationId());
    auditEventWriter.record(
        "TEMPLATE_VERSION_CREATED", "EMAIL_TEMPLATE", templateId, Map.of("version", nextVersion));
    return template(templateId);
  }

  @Transactional(readOnly = true)
  public RenderedTemplate preview(UUID templateVersionId, Map<String, String> values) {
    TemplateContent content = templateContent(templateVersionId);
    return renderer.render(content.subject(), content.textBody(), content.htmlBody(), values);
  }

  @Transactional(readOnly = true)
  public List<CampaignView> campaigns() {
    return jdbcTemplate.query(
        campaignSelect() + " WHERE c.organization_id = ? ORDER BY c.updated_at DESC, c.id DESC",
        this::campaignView,
        currentActor.organizationId());
  }

  @Transactional
  public CampaignView createCampaign(CreateCampaignCommand command) {
    TemplateView template = templateByVersion(command.templateVersionId());
    if (template.channel() != command.channel()) {
      throw new UnprocessableEntityException("Campaign and template channels must match");
    }
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO campaign (
          id, version, organization_id, name, description, objective, channel,
          owner_user_id, status, dry_run, daily_limit, approved, template_version_id,
          created_at, updated_at
        ) VALUES (?, 0, ?, ?, ?, ?, ?, ?, 'DRAFT', TRUE, 0, FALSE, ?, now(), now())
        """,
        id,
        currentActor.organizationId(),
        required(command.name(), "Campaign name"),
        trim(command.description()),
        trim(command.objective()),
        command.channel().name(),
        currentActor.userIdOrNull(),
        command.templateVersionId());
    auditEventWriter.record(
        "CAMPAIGN_CREATED", "CAMPAIGN", id, Map.of("channel", command.channel()));
    return campaign(id);
  }

  @Transactional
  public CampaignView freezeAudience(UUID campaignId, long version, AudienceFilter filter) {
    CampaignView campaign = campaign(campaignId);
    if (campaign.version() != version) {
      throw new OptimisticConflictException("Campaign was modified by another user");
    }
    if (!Set.of(CampaignState.DRAFT, CampaignState.READY_FOR_REVIEW).contains(campaign.status())) {
      throw new UnprocessableEntityException("Only draft campaigns can freeze an audience");
    }
    jdbcTemplate.update(
        "DELETE FROM campaign_audience_recipient WHERE organization_id = ? AND campaign_id = ?",
        currentActor.organizationId(),
        campaignId);

    List<AudienceCandidate> candidates = audienceCandidates(campaign.channel(), filter);
    if (candidates.size() > MAX_AUDIENCE) {
      throw new UnprocessableEntityException(
          "Audience reached the 10000 recipient safety limit; narrow the filters");
    }
    int included = 0;
    int excluded = 0;
    for (AudienceCandidate candidate : candidates) {
      AudienceDecision decision = decide(candidate);
      if (decision.included()) {
        included++;
      } else {
        excluded++;
      }
      jdbcTemplate.update(
          """
          INSERT INTO campaign_audience_recipient (
            id, organization_id, campaign_id, prospect_id, contact_id, contact_channel_id,
            included, exclusion_reason, channel, validation_status, frozen_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
          """,
          UUID.randomUUID(),
          currentActor.organizationId(),
          campaignId,
          candidate.prospectId(),
          candidate.contactId(),
          candidate.channelId(),
          decision.included(),
          decision.reason(),
          campaign.channel().name(),
          decision.status());
    }
    int updated =
        jdbcTemplate.update(
            """
            UPDATE campaign SET audience_filter = CAST(? AS jsonb), status = 'READY_FOR_REVIEW',
              frozen_at = now(), recipient_count = ?, excluded_count = ?, approved = FALSE,
              approved_by = NULL, approved_at = NULL, updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            json(filter),
            included,
            excluded,
            campaignId,
            currentActor.organizationId(),
            version);
    if (updated == 0) {
      throw new OptimisticConflictException("Campaign was modified by another user");
    }
    auditEventWriter.record(
        "CAMPAIGN_AUDIENCE_FROZEN",
        "CAMPAIGN",
        campaignId,
        Map.of("included", included, "excluded", excluded));
    return campaign(campaignId);
  }

  @Transactional(readOnly = true)
  public List<AudienceRecipientView> audience(UUID campaignId) {
    campaign(campaignId);
    return jdbcTemplate.query(
        """
        SELECT a.prospect_id, i.name AS display_name, a.contact_id,
          concat_ws(' ', c.first_name, c.last_name) AS contact_name,
          a.included, a.exclusion_reason, a.channel, a.validation_status, a.frozen_at
        FROM campaign_audience_recipient a
        JOIN prospect p ON p.id = a.prospect_id AND p.organization_id = a.organization_id
        JOIN institution i ON i.id = p.institution_id AND i.organization_id = p.organization_id
        LEFT JOIN contact c ON c.id = a.contact_id AND c.organization_id = a.organization_id
        WHERE a.organization_id = ? AND a.campaign_id = ?
        ORDER BY a.included DESC, lower(i.name), p.id
        """,
        this::audienceView,
        currentActor.organizationId(),
        campaignId);
  }

  @Transactional
  public CampaignView approve(UUID campaignId, long version) {
    CampaignView before = campaign(campaignId);
    if (before.version() != version) {
      throw new OptimisticConflictException("Campaign was modified by another user");
    }
    if (before.status() != CampaignState.READY_FOR_REVIEW) {
      throw new UnprocessableEntityException("Campaign must be ready for review");
    }
    if (before.recipientCount() <= 0) {
      throw new UnprocessableEntityException("Campaign has no valid recipients");
    }
    int updated =
        jdbcTemplate.update(
            """
            UPDATE campaign SET status = 'APPROVED', approved = TRUE, approved_by = ?,
              approved_at = now(), updated_at = now(), version = version + 1
            WHERE id = ? AND organization_id = ? AND version = ?
            """,
            currentActor.userIdOrNull(),
            campaignId,
            currentActor.organizationId(),
            version);
    if (updated == 0) {
      throw new OptimisticConflictException("Campaign was modified by another user");
    }
    auditEventWriter.record(
        "CAMPAIGN_APPROVED", "CAMPAIGN", campaignId, Map.of("recipients", before.recipientCount()));
    return campaign(campaignId);
  }

  @Transactional
  public SimulationView simulate(UUID campaignId, String idempotencyKey) {
    String key = required(idempotencyKey, "Idempotency key");
    SimulationView existing = simulationByKey(key);
    if (existing != null) {
      if (!existing.campaignId().equals(campaignId)) {
        throw new OptimisticConflictException("Idempotency key belongs to another campaign");
      }
      return existing;
    }
    CampaignView campaign = campaign(campaignId);
    if (!Set.of(CampaignState.APPROVED, CampaignState.SIMULATED).contains(campaign.status())) {
      throw new UnprocessableEntityException("Campaign must be approved before simulation");
    }
    requireSafeSendingConfiguration();
    TemplateContent template = templateContent(campaign.templateVersionId());
    UUID runId = UUID.randomUUID();
    List<SimulationCandidate> recipients = simulationCandidates(campaignId);
    int included = (int) recipients.stream().filter(SimulationCandidate::included).count();
    int excluded = recipients.size() - included;
    jdbcTemplate.update(
        """
        INSERT INTO campaign_simulation_run (
          id, organization_id, campaign_id, idempotency_key, included_count,
          excluded_count, status, created_by, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, 'SIMULATED', ?, now())
        """,
        runId,
        currentActor.organizationId(),
        campaignId,
        key,
        included,
        excluded,
        currentActor.userIdOrNull());
    for (SimulationCandidate recipient : recipients) {
      if (!recipient.included()) {
        simulationResult(runId, campaignId, recipient, "EXCLUDED", recipient.reason(), null);
        continue;
      }
      RenderedTemplate rendered =
          renderer.render(
              template.subject(),
              template.textBody(),
              template.htmlBody(),
              recipient.values(campaign.name()));
      simulationResult(runId, campaignId, recipient, "SIMULATED", null, rendered);
      recordDraftActivity(campaign, runId, recipient);
    }
    jdbcTemplate.update(
        """
        UPDATE campaign SET status = 'SIMULATED', simulated_at = now(), updated_at = now(),
          version = version + 1 WHERE id = ? AND organization_id = ?
        """,
        campaignId,
        currentActor.organizationId());
    auditEventWriter.record(
        "CAMPAIGN_SIMULATED",
        "CAMPAIGN",
        campaignId,
        Map.of("included", included, "excluded", excluded, "provider", "FAKE"));
    outboxPublisher.publish(
        new PublishCommand(
            currentActor.organizationId(),
            "CAMPAIGN_SIMULATED_V1",
            1,
            "CAMPAIGN",
            campaignId,
            Map.of(
                "campaignId",
                campaignId.toString(),
                "simulationRunId",
                runId.toString(),
                "included",
                included,
                "excluded",
                excluded),
            "campaign-simulation-result:" + runId,
            CorrelationIds.currentOrCreate(),
            currentActor.userIdOrNull(),
            3));
    return simulation(runId);
  }

  @Transactional
  public List<SequenceStepView> replaceSequence(
      UUID campaignId, long version, List<SequenceStepCommand> steps) {
    CampaignView campaign = campaign(campaignId);
    if (campaign.version() != version) {
      throw new OptimisticConflictException("Campaign was modified by another user");
    }
    if (campaign.status() != CampaignState.DRAFT) {
      throw new UnprocessableEntityException("Sequence can only change while campaign is draft");
    }
    validateSequence(steps);
    jdbcTemplate.update(
        "DELETE FROM campaign_sequence_step WHERE organization_id = ? AND campaign_id = ?",
        currentActor.organizationId(),
        campaignId);
    int order = 0;
    for (SequenceStepCommand step : steps) {
      order++;
      jdbcTemplate.update(
          """
          INSERT INTO campaign_sequence_step (
            id, organization_id, campaign_id, step_order, step_type, configuration, created_at
          ) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), now())
          """,
          UUID.randomUUID(),
          currentActor.organizationId(),
          campaignId,
          order,
          step.type().name(),
          json(step.configuration()));
    }
    jdbcTemplate.update(
        "UPDATE campaign SET version = version + 1, updated_at = now() WHERE id = ? AND organization_id = ? AND version = ?",
        campaignId,
        currentActor.organizationId(),
        version);
    auditEventWriter.record(
        "CAMPAIGN_SEQUENCE_CHANGED", "CAMPAIGN", campaignId, Map.of("steps", steps.size()));
    return sequence(campaignId);
  }

  @Transactional(readOnly = true)
  public List<SequenceStepView> sequence(UUID campaignId) {
    campaign(campaignId);
    return jdbcTemplate.query(
        """
        SELECT id, step_order, step_type, configuration::text AS configuration
        FROM campaign_sequence_step
        WHERE organization_id = ? AND campaign_id = ? ORDER BY step_order
        """,
        (rs, rowNum) ->
            new SequenceStepView(
                rs.getObject("id", UUID.class),
                rs.getInt("step_order"),
                SequenceStepType.valueOf(rs.getString("step_type")),
                map(rs.getString("configuration"))),
        currentActor.organizationId(),
        campaignId);
  }

  private List<AudienceCandidate> audienceCandidates(
      CampaignChannel channel, AudienceFilter filter) {
    List<Object> parameters = new ArrayList<>();
    parameters.add(channel.name());
    parameters.add(channel.name());
    parameters.add(currentActor.organizationId());
    StringBuilder where =
        new StringBuilder(" WHERE p.organization_id = ? AND p.archived_at IS NULL");
    if (filter.status() != null && !filter.status().isBlank()) {
      where.append(" AND p.status = ?");
      parameters.add(filter.status());
    }
    if (filter.eligibility() != null && !filter.eligibility().isBlank()) {
      where.append(" AND p.eligibility = ?");
      parameters.add(filter.eligibility());
    }
    if (filter.priorityAtLeast() != null) {
      where.append(" AND p.priority >= ?");
      parameters.add(filter.priorityAtLeast());
    }
    if (filter.scoreAtLeast() != null) {
      where.append(" AND p.score >= ?");
      parameters.add(filter.scoreAtLeast());
    }
    if (filter.province() != null && !filter.province().isBlank()) {
      where.append(" AND lower(i.province) = lower(?)");
      parameters.add(filter.province().trim());
    }
    if (filter.ownerId() != null) {
      where.append(" AND p.owner_user_id = ?");
      parameters.add(filter.ownerId());
    }
    if (filter.excludeCustomers()) {
      where.append(" AND p.status <> 'CUSTOMER'");
    }
    if (filter.requireActiveOpportunity()) {
      where.append(
          " AND EXISTS (SELECT 1 FROM opportunity o WHERE o.organization_id = p.organization_id AND o.prospect_id = p.id AND o.stage NOT IN ('WON', 'LOST'))");
    }
    parameters.add(MAX_AUDIENCE + 1);
    return jdbcTemplate.query(
        """
        SELECT p.id AS prospect_id, i.name AS display_name, p.eligibility, p.contact_eligible,
          p.status, selected.contact_id, selected.channel_id, selected.normalized_value,
          selected.first_name, selected.last_name, owner.display_name AS owner_name,
          i.locality AS city,
          EXISTS (
            SELECT 1 FROM exclusion e WHERE e.organization_id = p.organization_id
              AND e.channel_type = ? AND e.normalized_value = selected.normalized_value
          ) AS excluded
        FROM prospect p
        JOIN institution i ON i.id = p.institution_id AND i.organization_id = p.organization_id
        LEFT JOIN app_user owner ON owner.id = p.owner_user_id
        LEFT JOIN LATERAL (
          SELECT c.id AS contact_id, cc.id AS channel_id, cc.normalized_value,
            c.first_name, c.last_name
          FROM contact c
          JOIN contact_channel cc ON cc.contact_id = c.id AND cc.organization_id = c.organization_id
          WHERE c.organization_id = p.organization_id AND c.institution_id = p.institution_id
            AND c.deleted_at IS NULL AND cc.type = ? AND cc.valid
            AND c.consent <> 'DENIED' AND cc.consent <> 'DENIED'
          ORDER BY cc.preferred DESC, cc.primary_channel DESC, c.primary_contact DESC, cc.created_at
          LIMIT 1
        ) selected ON TRUE
        """
            + where
            + " ORDER BY p.id LIMIT ?",
        this::audienceCandidate,
        parameters.toArray());
  }

  private AudienceDecision decide(AudienceCandidate candidate) {
    if (!candidate.contactEligible()
        || !"ELIGIBLE".equals(candidate.eligibility())
        || "CUSTOMER".equals(candidate.status())) {
      return new AudienceDecision(false, "Prospect is not contact eligible", "INELIGIBLE");
    }
    if (candidate.channelId() == null) {
      return new AudienceDecision(false, "No valid consented channel", "MISSING_CHANNEL");
    }
    if (candidate.excluded()) {
      return new AudienceDecision(false, "Channel is present in exclusion registry", "EXCLUDED");
    }
    return new AudienceDecision(true, null, "VALID");
  }

  private List<SimulationCandidate> simulationCandidates(UUID campaignId) {
    return jdbcTemplate.query(
        """
        SELECT a.prospect_id, a.contact_id, a.included, a.exclusion_reason,
          i.name AS display_name, i.locality AS city, c.first_name, c.last_name,
          owner.display_name AS owner_name
        FROM campaign_audience_recipient a
        JOIN prospect p ON p.id = a.prospect_id AND p.organization_id = a.organization_id
        JOIN institution i ON i.id = p.institution_id AND i.organization_id = p.organization_id
        LEFT JOIN contact c ON c.id = a.contact_id AND c.organization_id = a.organization_id
        LEFT JOIN app_user owner ON owner.id = p.owner_user_id
        WHERE a.organization_id = ? AND a.campaign_id = ?
        ORDER BY p.id
        """,
        (rs, rowNum) ->
            new SimulationCandidate(
                rs.getObject("prospect_id", UUID.class),
                rs.getObject("contact_id", UUID.class),
                rs.getBoolean("included"),
                rs.getString("exclusion_reason"),
                rs.getString("display_name"),
                rs.getString("city"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("owner_name")),
        currentActor.organizationId(),
        campaignId);
  }

  private void simulationResult(
      UUID runId,
      UUID campaignId,
      SimulationCandidate recipient,
      String result,
      String reason,
      RenderedTemplate rendered) {
    jdbcTemplate.update(
        """
        INSERT INTO campaign_simulation_result (
          id, organization_id, simulation_run_id, campaign_id, prospect_id, contact_id,
          result, reason, rendered_subject_hash, rendered_body_hash, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
        """,
        UUID.randomUUID(),
        currentActor.organizationId(),
        runId,
        campaignId,
        recipient.prospectId(),
        recipient.contactId(),
        result,
        reason,
        rendered == null ? null : sha256(rendered.subject()),
        rendered == null ? null : sha256(rendered.textBody() + rendered.htmlBody()));
  }

  private void recordDraftActivity(
      CampaignView campaign, UUID runId, SimulationCandidate recipient) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("campaignId", campaign.id().toString());
    metadata.put("simulationRunId", runId.toString());
    metadata.put("provider", "FAKE");
    jdbcTemplate.update(
        """
        INSERT INTO activity (
          id, organization_id, prospect_id, contact_id, actor_user_id, activity_type,
          occurred_at, channel, direction, outcome, summary, detail, external_reference,
          metadata, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, now(), ?, 'OUTBOUND', 'SIMULATED', ?, ?, ?, CAST(? AS jsonb), now())
        """,
        UUID.randomUUID(),
        currentActor.organizationId(),
        recipient.prospectId(),
        recipient.contactId(),
        currentActor.userIdOrNull(),
        campaign.channel() == CampaignChannel.EMAIL ? "EMAIL_DRAFTED" : "WHATSAPP_DRAFTED",
        campaign.channel().name(),
        "Campaign simulated: " + campaign.name(),
        "Fake provider only; no network dispatch",
        "campaign-simulation:" + runId + ":" + recipient.prospectId(),
        json(metadata));
  }

  private void requireSafeSendingConfiguration() {
    if (!sendingProperties.blocksRealSending()) {
      throw new UnprocessableEntityException("Simulation requires environment sending blockade");
    }
    Map<String, String> settings =
        jdbcTemplate.query(
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
    if (!"true".equalsIgnoreCase(settings.get("sending.kill-switch"))
        || !"false".equalsIgnoreCase(settings.get("sending.enabled"))
        || !"true".equalsIgnoreCase(settings.get("sending.dry-run"))
        || !"0".equals(settings.get("sending.daily-limit"))) {
      throw new UnprocessableEntityException("Persistent sending blockade is incomplete");
    }
  }

  private void insertVersion(
      UUID templateId, int version, String subject, String textBody, String htmlBody) {
    Set<String> variables = renderer.variables(subject, textBody, htmlBody);
    jdbcTemplate.update(
        """
        INSERT INTO template_version (
          id, version, organization_id, template_id, version_number, subject,
          html_body, text_body, variables, created_at, updated_at
        ) VALUES (?, 0, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), now(), now())
        """,
        UUID.randomUUID(),
        currentActor.organizationId(),
        templateId,
        version,
        required(subject, "Subject"),
        required(htmlBody, "HTML body"),
        required(textBody, "Text body"),
        json(variables));
  }

  private void validateTemplate(String subject, String textBody, String htmlBody) {
    renderer.render(
        required(subject, "Subject"),
        required(textBody, "Text body"),
        required(htmlBody, "HTML body"),
        SafeTemplateRenderer.ALLOWED_VARIABLES.stream()
            .collect(java.util.stream.Collectors.toMap(value -> value, value -> "Synthetic")));
  }

  private void validateSequence(List<SequenceStepCommand> steps) {
    if (steps == null || steps.isEmpty() || steps.size() > 20) {
      throw new UnprocessableEntityException("Sequence must have between 1 and 20 steps");
    }
    for (SequenceStepCommand step : steps) {
      Map<String, Object> configuration = step.configuration();
      if (step.type() == SequenceStepType.WAIT
          && !(configuration.get("days") instanceof Number days
              && days.intValue() >= 1
              && days.intValue() <= 90)) {
        throw new UnprocessableEntityException("WAIT step requires days between 1 and 90");
      }
      if (step.type() == SequenceStepType.CONDITION) {
        Object condition = configuration.get("condition");
        if (!Set.of("REPLIED", "DO_NOT_CONTACT", "CUSTOMER", "NO_VALID_CHANNEL")
            .contains(condition)) {
          throw new UnprocessableEntityException("Unsupported sequence condition");
        }
      }
      if (!Set.of("days", "condition", "action", "title").containsAll(configuration.keySet())) {
        throw new UnprocessableEntityException("Sequence contains unsupported configuration keys");
      }
    }
  }

  private CampaignView campaign(UUID id) {
    return jdbcTemplate
        .query(
            campaignSelect() + " WHERE c.organization_id = ? AND c.id = ?",
            this::campaignView,
            currentActor.organizationId(),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));
  }

  private TemplateView template(UUID id) {
    return templates().stream()
        .filter(item -> item.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
  }

  private TemplateView templateByVersion(UUID versionId) {
    return jdbcTemplate
        .query(
            """
            SELECT t.id, t.name, t.channel, t.archived_at,
              v.id AS version_id, v.version_number, v.subject, v.text_body, v.html_body,
              v.variables::text AS variables, v.created_at
            FROM template_version v
            JOIN email_template t ON t.id = v.template_id AND t.organization_id = v.organization_id
            WHERE v.organization_id = ? AND v.id = ? AND v.archived_at IS NULL AND t.archived_at IS NULL
            """,
            this::templateView,
            currentActor.organizationId(),
            versionId)
        .stream()
        .findFirst()
        .orElseThrow(
            () -> new ResourceNotFoundException("Template version not found: " + versionId));
  }

  private TemplateContent templateContent(UUID versionId) {
    TemplateView view = templateByVersion(versionId);
    return new TemplateContent(view.subject(), view.textBody(), view.htmlBody());
  }

  private SimulationView simulationByKey(String key) {
    return jdbcTemplate
        .query(
            """
            SELECT id, campaign_id, idempotency_key, included_count, excluded_count, status, created_at
            FROM campaign_simulation_run WHERE organization_id = ? AND idempotency_key = ?
            """,
            this::simulationView,
            currentActor.organizationId(),
            key)
        .stream()
        .findFirst()
        .orElse(null);
  }

  private SimulationView simulation(UUID id) {
    return jdbcTemplate.queryForObject(
        """
        SELECT id, campaign_id, idempotency_key, included_count, excluded_count, status, created_at
        FROM campaign_simulation_run WHERE organization_id = ? AND id = ?
        """,
        this::simulationView,
        currentActor.organizationId(),
        id);
  }

  private String campaignSelect() {
    return """
        SELECT c.id, c.version, c.name, c.description, c.objective, c.channel,
          c.status, c.dry_run, c.daily_limit, c.approved, c.template_version_id,
          t.name AS template_name, c.recipient_count, c.excluded_count, c.frozen_at,
          c.approved_at, c.simulated_at, c.created_at, c.updated_at
        FROM campaign c
        LEFT JOIN template_version tv ON tv.id = c.template_version_id AND tv.organization_id = c.organization_id
        LEFT JOIN email_template t ON t.id = tv.template_id AND t.organization_id = c.organization_id
        """;
  }

  private CampaignView campaignView(ResultSet rs, int rowNum) throws SQLException {
    return new CampaignView(
        rs.getObject("id", UUID.class),
        rs.getLong("version"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getString("objective"),
        CampaignChannel.valueOf(rs.getString("channel")),
        CampaignState.valueOf(rs.getString("status")),
        rs.getBoolean("dry_run"),
        rs.getInt("daily_limit"),
        rs.getBoolean("approved"),
        rs.getObject("template_version_id", UUID.class),
        rs.getString("template_name"),
        rs.getInt("recipient_count"),
        rs.getInt("excluded_count"),
        instant(rs, "frozen_at"),
        instant(rs, "approved_at"),
        instant(rs, "simulated_at"),
        instant(rs, "created_at"),
        instant(rs, "updated_at"));
  }

  private TemplateView templateView(ResultSet rs, int rowNum) throws SQLException {
    return new TemplateView(
        rs.getObject("id", UUID.class),
        rs.getString("name"),
        CampaignChannel.valueOf(rs.getString("channel")),
        rs.getObject("version_id", UUID.class),
        rs.getInt("version_number"),
        rs.getString("subject"),
        rs.getString("text_body"),
        rs.getString("html_body"),
        list(rs.getString("variables")),
        instant(rs, "created_at"));
  }

  private AudienceCandidate audienceCandidate(ResultSet rs, int rowNum) throws SQLException {
    return new AudienceCandidate(
        rs.getObject("prospect_id", UUID.class),
        rs.getObject("contact_id", UUID.class),
        rs.getObject("channel_id", UUID.class),
        rs.getString("eligibility"),
        rs.getBoolean("contact_eligible"),
        rs.getString("status"),
        rs.getBoolean("excluded"));
  }

  private AudienceRecipientView audienceView(ResultSet rs, int rowNum) throws SQLException {
    return new AudienceRecipientView(
        rs.getObject("prospect_id", UUID.class),
        rs.getString("display_name"),
        rs.getObject("contact_id", UUID.class),
        rs.getString("contact_name"),
        rs.getBoolean("included"),
        rs.getString("exclusion_reason"),
        CampaignChannel.valueOf(rs.getString("channel")),
        rs.getString("validation_status"),
        instant(rs, "frozen_at"));
  }

  private SimulationView simulationView(ResultSet rs, int rowNum) throws SQLException {
    return new SimulationView(
        rs.getObject("id", UUID.class),
        rs.getObject("campaign_id", UUID.class),
        rs.getString("idempotency_key"),
        rs.getInt("included_count"),
        rs.getInt("excluded_count"),
        rs.getString("status"),
        instant(rs, "created_at"));
  }

  private String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required");
    }
    return value.trim();
  }

  private String trim(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private Instant instant(ResultSet rs, String column) throws SQLException {
    return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toInstant();
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Value cannot be serialized", exception);
    }
  }

  private List<String> list(String value) {
    try {
      return value == null
          ? List.of()
          : objectMapper.readValue(value, new TypeReference<List<String>>() {});
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Stored template variables are invalid", exception);
    }
  }

  private Map<String, Object> map(String value) {
    try {
      return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Stored sequence configuration is invalid", exception);
    }
  }

  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private record AudienceCandidate(
      UUID prospectId,
      UUID contactId,
      UUID channelId,
      String eligibility,
      boolean contactEligible,
      String status,
      boolean excluded) {}

  private record AudienceDecision(boolean included, String reason, String status) {}

  private record SimulationCandidate(
      UUID prospectId,
      UUID contactId,
      boolean included,
      String reason,
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

  public record CreateTemplateCommand(
      String name, CampaignChannel channel, String subject, String textBody, String htmlBody) {}

  public record TemplateContent(String subject, String textBody, String htmlBody) {}

  public record TemplateView(
      UUID id,
      String name,
      CampaignChannel channel,
      UUID versionId,
      int versionNumber,
      String subject,
      String textBody,
      String htmlBody,
      List<String> variables,
      Instant createdAt) {}

  public record CreateCampaignCommand(
      String name,
      String description,
      String objective,
      CampaignChannel channel,
      UUID templateVersionId) {}

  public record AudienceFilter(
      String status,
      String eligibility,
      Integer priorityAtLeast,
      Integer scoreAtLeast,
      String province,
      UUID ownerId,
      boolean excludeCustomers,
      boolean requireActiveOpportunity) {}

  public record CampaignView(
      UUID id,
      long version,
      String name,
      String description,
      String objective,
      CampaignChannel channel,
      CampaignState status,
      boolean dryRun,
      int dailyLimit,
      boolean approved,
      UUID templateVersionId,
      String templateName,
      int recipientCount,
      int excludedCount,
      Instant frozenAt,
      Instant approvedAt,
      Instant simulatedAt,
      Instant createdAt,
      Instant updatedAt) {}

  public record AudienceRecipientView(
      UUID prospectId,
      String prospectName,
      UUID contactId,
      String contactName,
      boolean included,
      String exclusionReason,
      CampaignChannel channel,
      String validationStatus,
      Instant frozenAt) {}

  public record SimulationView(
      UUID id,
      UUID campaignId,
      String idempotencyKey,
      int includedCount,
      int excludedCount,
      String status,
      Instant createdAt) {}

  public record SequenceStepCommand(SequenceStepType type, Map<String, Object> configuration) {}

  public record SequenceStepView(
      UUID id, int order, SequenceStepType type, Map<String, Object> configuration) {}
}
