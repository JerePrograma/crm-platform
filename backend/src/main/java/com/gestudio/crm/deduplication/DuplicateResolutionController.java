package com.gestudio.crm.deduplication;

import com.gestudio.crm.deduplication.DuplicateResolutionService.ResolutionCommand;
import com.gestudio.crm.deduplication.DuplicateResolutionService.ResolutionResult;
import com.gestudio.crm.deduplication.DuplicateResolutionService.ReviewView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/duplicate-reviews")
public class DuplicateResolutionController {

  private final DuplicateResolutionService duplicateResolutionService;

  public DuplicateResolutionController(DuplicateResolutionService duplicateResolutionService) {
    this.duplicateResolutionService = duplicateResolutionService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('DUPLICATE_RESOLVE')")
  public List<ReviewView> queue() {
    return duplicateResolutionService.queue();
  }

  @GetMapping("/{reviewId}")
  @PreAuthorize("hasAuthority('DUPLICATE_RESOLVE')")
  public ReviewView get(@PathVariable UUID reviewId) {
    return duplicateResolutionService.get(reviewId);
  }

  @PostMapping("/{reviewId}/resolution")
  @PreAuthorize("hasAuthority('DUPLICATE_RESOLVE')")
  public ResolutionResult resolve(
      @PathVariable UUID reviewId, @Valid @RequestBody ResolutionRequest request) {
    return duplicateResolutionService.resolve(reviewId, request.command());
  }

  public record ResolutionRequest(
      @NotNull DuplicateResolutionAction action,
      UUID survivorProspectId,
      UUID absorbedProspectId,
      String separateName,
      String comment,
      @NotBlank String idempotencyKey) {
    ResolutionCommand command() {
      return new ResolutionCommand(
          action, survivorProspectId, absorbedProspectId, separateName, comment, idempotencyKey);
    }
  }
}
