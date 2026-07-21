package com.gestudio.crm.sales;

import com.gestudio.crm.sales.OpportunityService.CreateOpportunityCommand;
import com.gestudio.crm.sales.OpportunityService.OpportunityView;
import com.gestudio.crm.sales.OpportunityService.PipelineMetrics;
import com.gestudio.crm.sales.OpportunityService.StageHistoryView;
import com.gestudio.crm.sales.OpportunityService.TransitionCommand;
import com.gestudio.crm.sales.OpportunityService.UpdateOpportunityCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

  private final OpportunityService opportunityService;

  public OpportunityController(OpportunityService opportunityService) {
    this.opportunityService = opportunityService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  public List<OpportunityView> list(
      @RequestParam(required = false) UUID prospectId,
      @RequestParam(required = false) UUID ownerId,
      @RequestParam(required = false) OpportunityStage stage) {
    return opportunityService.list(prospectId, ownerId, stage);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  public OpportunityView get(@PathVariable UUID id) {
    return opportunityService.get(id);
  }

  @GetMapping("/{id}/history")
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  public List<StageHistoryView> history(@PathVariable UUID id) {
    return opportunityService.history(id);
  }

  @GetMapping("/metrics")
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public PipelineMetrics metrics() {
    return opportunityService.metrics();
  }

  @PostMapping
  @PreAuthorize("hasAuthority('OPPORTUNITY_WRITE')")
  public ResponseEntity<OpportunityView> create(@Valid @RequestBody OpportunityRequest request) {
    OpportunityView created = opportunityService.create(request.createCommand());
    return ResponseEntity.created(URI.create("/api/v1/opportunities/" + created.id()))
        .body(created);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('OPPORTUNITY_WRITE')")
  public OpportunityView update(
      @PathVariable UUID id, @Valid @RequestBody OpportunityRequest request) {
    return opportunityService.update(id, request.updateCommand());
  }

  @PostMapping("/{id}/transitions")
  @PreAuthorize("hasAuthority('OPPORTUNITY_WRITE')")
  public OpportunityView transition(
      @PathVariable UUID id, @Valid @RequestBody TransitionRequest request) {
    return opportunityService.transition(id, request.command());
  }

  public record OpportunityRequest(
      @PositiveOrZero long version,
      @NotNull UUID prospectId,
      @NotBlank String name,
      @NotNull UUID ownerId,
      @NotNull @DecimalMin("0.00") BigDecimal estimatedValue,
      @NotBlank String currency,
      @Min(0) @Max(100) int probability,
      LocalDate expectedCloseDate,
      String source,
      boolean primaryActive) {
    CreateOpportunityCommand createCommand() {
      return new CreateOpportunityCommand(
          prospectId,
          name,
          ownerId,
          estimatedValue,
          currency,
          probability,
          expectedCloseDate,
          source,
          primaryActive);
    }

    UpdateOpportunityCommand updateCommand() {
      return new UpdateOpportunityCommand(
          version,
          name,
          ownerId,
          estimatedValue,
          currency,
          probability,
          expectedCloseDate,
          source,
          primaryActive);
    }
  }

  public record TransitionRequest(
      @PositiveOrZero long version,
      @NotNull OpportunityStage stage,
      String reason,
      String comment) {
    TransitionCommand command() {
      return new TransitionCommand(version, stage, reason, comment);
    }
  }
}
