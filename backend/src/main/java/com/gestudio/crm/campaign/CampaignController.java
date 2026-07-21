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
import com.gestudio.crm.campaign.SafeTemplateRenderer.RenderedTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
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

  public CampaignController(CampaignService campaignService) {
    this.campaignService = campaignService;
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
      @NotNull UUID templateVersionId) {
    CreateCampaignCommand command() {
      return new CreateCampaignCommand(name, description, objective, channel, templateVersionId);
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
