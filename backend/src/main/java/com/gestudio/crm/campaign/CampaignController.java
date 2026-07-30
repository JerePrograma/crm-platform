package com.gestudio.crm.campaign;

import com.gestudio.crm.campaign.CampaignService.AudienceFilter;
import com.gestudio.crm.campaign.CampaignService.AudienceRecipientView;
import com.gestudio.crm.campaign.CampaignService.CampaignView;
import com.gestudio.crm.campaign.CampaignService.CreateCampaignCommand;
import com.gestudio.crm.campaign.CampaignService.CreateTemplateCommand;
import com.gestudio.crm.campaign.CampaignService.SequenceStepCommand;
import com.gestudio.crm.campaign.CampaignService.SequenceStepView;
import com.gestudio.crm.campaign.CampaignService.SimulationView;
import com.gestudio.crm.campaign.CampaignService.TemplateContent;
import com.gestudio.crm.campaign.CampaignService.TemplateView;
import com.gestudio.crm.campaign.CampaignService.UpdateDeliveryCommand;
import com.gestudio.crm.campaign.SafeTemplateRenderer.RenderedTemplate;
import com.gestudio.crm.common.CsvSafety;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CampaignController {

  private final CampaignService campaignService;
  private final CampaignDeliveryService campaignDeliveryService;

  public CampaignController(
      CampaignService campaignService, CampaignDeliveryService campaignDeliveryService) {
    this.campaignService = campaignService;
    this.campaignDeliveryService = campaignDeliveryService;
  }

  @GetMapping("/templates")
  @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
  public List<TemplateView> templates() {
    return campaignService.templates();
  }

  @PostMapping("/templates")
  @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
  public ResponseEntity<TemplateView> createTemplate(@Valid @RequestBody TemplateRequest request) {
    TemplateView created = campaignService.createTemplate(request.createCommand());
    return ResponseEntity.created(URI.create("/api/v1/templates/" + created.id())).body(created);
  }

  @PostMapping("/templates/{id}/versions")
  @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
  public TemplateView addTemplateVersion(
      @PathVariable UUID id, @Valid @RequestBody TemplateContentRequest request) {
    return campaignService.addTemplateVersion(id, request.command());
  }

  @PostMapping("/template-versions/{id}/preview")
  @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
  public RenderedTemplate preview(
      @PathVariable UUID id, @RequestBody Map<String, String> variables) {
    return campaignService.preview(id, variables);
  }

  @GetMapping("/campaigns")
  @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
  public List<CampaignView> campaigns() {
    return campaignService.campaigns();
  }

  @PostMapping("/campaigns")
  @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
  public ResponseEntity<CampaignView> createCampaign(@Valid @RequestBody CampaignRequest request) {
    CampaignView created = campaignService.createCampaign(request.command());
    return ResponseEntity.created(URI.create("/api/v1/campaigns/" + created.id())).body(created);
  }

  @PostMapping("/campaigns/{id}/audience/freeze")
  @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
  public CampaignView freezeAudience(
      @PathVariable UUID id, @Valid @RequestBody AudienceRequest request) {
    return campaignService.freezeAudience(id, request.version(), request.filter());
  }

  @PutMapping("/campaigns/{id}/delivery")
  @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
  public CampaignView updateDelivery(
      @PathVariable UUID id, @Valid @RequestBody DeliveryConfigurationRequest request) {
    return campaignService.updateDelivery(id, request.command());
  }

  @GetMapping("/campaigns/{id}/audience")
  @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
  public List<AudienceRecipientView> audience(@PathVariable UUID id) {
    return campaignService.audience(id);
  }

  @PostMapping("/campaigns/{id}/approve")
  @PreAuthorize("hasAuthority('CAMPAIGN_APPROVE')")
  public CampaignView approve(@PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
    return campaignService.approve(id, request.version());
  }

  @PostMapping("/campaigns/{id}/simulate")
  @PreAuthorize("hasAuthority('MESSAGE_SIMULATE')")
  public SimulationView simulate(
      @PathVariable UUID id, @RequestHeader("Idempotency-Key") String idempotencyKey) {
    return campaignService.simulate(id, idempotencyKey);
  }

  @PostMapping("/campaigns/{id}/schedule")
  @PreAuthorize("hasAuthority('MESSAGE_SEND')")
  public CampaignDeliveryService.CampaignProgress schedule(
      @PathVariable UUID id, @Valid @RequestBody ScheduleRequest request) {
    return campaignDeliveryService.schedule(
        id, request.version(), request.confirmation(), request.scheduledAt());
  }

  @PostMapping("/campaigns/{id}/start")
  @PreAuthorize("hasAuthority('MESSAGE_SEND')")
  public CampaignDeliveryService.CampaignProgress start(
      @PathVariable UUID id, @Valid @RequestBody LiveActionRequest request) {
    return campaignDeliveryService.start(id, request.version(), request.confirmation());
  }

  @PostMapping("/campaigns/{id}/pause")
  @PreAuthorize("hasAuthority('MESSAGE_SEND')")
  public CampaignDeliveryService.CampaignProgress pause(
      @PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
    return campaignDeliveryService.pause(id, request.version());
  }

  @PostMapping("/campaigns/{id}/resume")
  @PreAuthorize("hasAuthority('MESSAGE_SEND')")
  public CampaignDeliveryService.CampaignProgress resume(
      @PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
    return campaignDeliveryService.resume(id, request.version());
  }

  @PostMapping("/campaigns/{id}/cancel")
  @PreAuthorize("hasAuthority('MESSAGE_SEND')")
  public CampaignDeliveryService.CampaignProgress cancel(
      @PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
    return campaignDeliveryService.cancel(id, request.version());
  }

  @GetMapping("/campaigns/{id}/progress")
  @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
  public CampaignDeliveryService.CampaignProgress progress(@PathVariable UUID id) {
    return campaignDeliveryService.progress(id);
  }

  @GetMapping("/campaigns/{id}/results")
  @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
  public List<CampaignDeliveryService.RecipientResult> results(@PathVariable UUID id) {
    return campaignDeliveryService.results(id);
  }

  @GetMapping(value = "/campaigns/{id}/results.csv", produces = "text/csv")
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public ResponseEntity<String> resultsCsv(@PathVariable UUID id) {
    StringBuilder csv =
        new StringBuilder(
            "message_id,prospect_id,recipient,status,category,attempts,accepted_at,http_status,error\r\n");
    for (CampaignDeliveryService.RecipientResult result : campaignDeliveryService.results(id)) {
      csv.append(CsvSafety.cell(result.messageId()))
          .append(',')
          .append(CsvSafety.cell(result.prospectId()))
          .append(',')
          .append(CsvSafety.cell(result.maskedRecipient()))
          .append(',')
          .append(CsvSafety.cell(result.status()))
          .append(',')
          .append(CsvSafety.cell(result.resultCategory()))
          .append(',')
          .append(CsvSafety.cell(result.attempts()))
          .append(',')
          .append(CsvSafety.cell(result.acceptedAt()))
          .append(',')
          .append(CsvSafety.cell(result.httpStatus()))
          .append(',')
          .append(CsvSafety.cell(result.error()))
          .append("\r\n");
    }
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=campaign-results-" + id + ".csv")
        .body(csv.toString());
  }

  @PutMapping("/campaigns/{id}/sequence")
  @PreAuthorize("hasAuthority('CAMPAIGN_WRITE')")
  public List<SequenceStepView> replaceSequence(
      @PathVariable UUID id, @Valid @RequestBody SequenceRequest request) {
    return campaignService.replaceSequence(id, request.version(), request.commands());
  }

  @GetMapping("/campaigns/{id}/sequence")
  @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
  public List<SequenceStepView> sequence(@PathVariable UUID id) {
    return campaignService.sequence(id);
  }

  public record TemplateRequest(
      @NotBlank @Size(max = 160) String name,
      @NotNull CampaignChannel channel,
      @NotBlank @Size(max = 300) String subject,
      @NotBlank @Size(max = 50_000) String textBody,
      @NotBlank @Size(max = 100_000) String htmlBody) {
    CreateTemplateCommand createCommand() {
      return new CreateTemplateCommand(name, channel, subject, textBody, htmlBody);
    }
  }

  public record TemplateContentRequest(
      @NotBlank @Size(max = 300) String subject,
      @NotBlank @Size(max = 50_000) String textBody,
      @NotBlank @Size(max = 100_000) String htmlBody) {
    TemplateContent command() {
      return new TemplateContent(subject, textBody, htmlBody);
    }
  }

  public record CampaignRequest(
      @NotBlank @Size(max = 200) String name,
      @Size(max = 2000) String description,
      @Size(max = 500) String objective,
      @NotNull CampaignChannel channel,
      @NotNull UUID templateVersionId,
      CampaignExecutionMode executionMode,
      UUID senderAccountId,
      @Size(max = 320) String replyTo,
      @Size(max = 80) String timezone,
      @Size(max = 8) String operatingWindowStart,
      @Size(max = 8) String operatingWindowEnd,
      @Size(max = 7) List<@Min(1) @Max(7) Integer> businessDays,
      @Min(0) @Max(10) Integer dailyLimit,
      @Min(1) @Max(86_400) Integer minimumIntervalSeconds,
      @Min(1) @Max(20) Integer maxAttempts,
      Map<String, Object> stopConfiguration) {
    CreateCampaignCommand command() {
      return new CreateCampaignCommand(
          name,
          description,
          objective,
          channel,
          templateVersionId,
          executionMode,
          senderAccountId,
          replyTo,
          timezone,
          operatingWindowStart,
          operatingWindowEnd,
          businessDays,
          dailyLimit,
          minimumIntervalSeconds,
          maxAttempts,
          stopConfiguration);
    }
  }

  public record DeliveryConfigurationRequest(
      @PositiveOrZero long version,
      @NotNull UUID templateVersionId,
      @NotNull CampaignExecutionMode executionMode,
      UUID senderAccountId,
      @Size(max = 320) String replyTo,
      @NotBlank @Size(max = 80) String timezone,
      @NotBlank @Size(max = 8) String operatingWindowStart,
      @NotBlank @Size(max = 8) String operatingWindowEnd,
      @NotEmpty @Size(max = 7) List<@Min(1) @Max(7) Integer> businessDays,
      @Min(0) @Max(10) int dailyLimit,
      @Min(1) @Max(86_400) int minimumIntervalSeconds,
      @Min(1) @Max(20) int maxAttempts,
      Map<String, Object> stopConfiguration) {
    UpdateDeliveryCommand command() {
      return new UpdateDeliveryCommand(
          version,
          templateVersionId,
          executionMode,
          senderAccountId,
          replyTo,
          timezone,
          operatingWindowStart,
          operatingWindowEnd,
          businessDays,
          dailyLimit,
          minimumIntervalSeconds,
          maxAttempts,
          stopConfiguration);
    }
  }

  public record AudienceRequest(
      @PositiveOrZero long version,
      String status,
      String eligibility,
      @Min(1) @Max(5) Integer priorityAtLeast,
      @Min(0) @Max(100) Integer scoreAtLeast,
      @Size(max = 160) String province,
      UUID ownerId,
      boolean excludeCustomers,
      boolean requireActiveOpportunity) {
    AudienceFilter filter() {
      return new AudienceFilter(
          status,
          eligibility,
          priorityAtLeast,
          scoreAtLeast,
          province,
          ownerId,
          excludeCustomers,
          requireActiveOpportunity);
    }
  }

  public record VersionRequest(@PositiveOrZero long version) {}

  public record LiveActionRequest(
      @PositiveOrZero long version, @NotBlank @Size(max = 64) String confirmation) {}

  public record ScheduleRequest(
      @PositiveOrZero long version,
      @NotBlank @Size(max = 64) String confirmation,
      @NotNull Instant scheduledAt) {}

  public record SequenceRequest(
      @PositiveOrZero long version, @NotEmpty @Size(max = 20) List<@Valid StepRequest> steps) {
    List<SequenceStepCommand> commands() {
      return steps.stream().map(StepRequest::command).toList();
    }
  }

  public record StepRequest(
      @NotNull SequenceStepType type, @NotNull Map<String, Object> configuration) {
    SequenceStepCommand command() {
      return new SequenceStepCommand(type, configuration);
    }
  }
}
