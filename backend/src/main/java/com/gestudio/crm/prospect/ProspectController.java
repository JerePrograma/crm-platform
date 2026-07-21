package com.gestudio.crm.prospect;

import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.ProspectApplicationService.ProspectView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prospects")
@Tag(name = "Prospects")
public class ProspectController {

  private final ProspectApplicationService prospectApplicationService;

  public ProspectController(ProspectApplicationService prospectApplicationService) {
    this.prospectApplicationService = prospectApplicationService;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  @Operation(summary = "Create a prospect after normalization and exclusion checks")
  public ResponseEntity<ProspectView> create(@Valid @RequestBody CreateProspectRequest request) {
    ProspectView created = prospectApplicationService.create(request.toCommand());
    return ResponseEntity.created(URI.create("/api/v1/prospects/" + created.id())).body(created);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  @Operation(summary = "Get the integral prospect summary")
  public ProspectView get(@PathVariable UUID id) {
    return prospectApplicationService.get(id);
  }

  @GetMapping
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  @Operation(summary = "List prospects with pagination and optional status filtering")
  public Page<ProspectView> list(
      @RequestParam(required = false) ProspectStatus status,
      @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
    return prospectApplicationService.list(status, pageable);
  }

  public record CreateProspectRequest(
      @NotBlank String institutionName,
      String category,
      String locality,
      String province,
      String country,
      String website,
      String contactName,
      String contactRole,
      @Email String email,
      String phone,
      String whatsapp,
      String externalSourceId,
      String source,
      String evidence,
      @PositiveOrZero Integer estimatedStudents,
      @PositiveOrZero Integer priority,
      @PositiveOrZero Integer score,
      String currentTools,
      String administrativePain,
      Instant verifiedAt,
      String owner) {

    CreateProspectCommand toCommand() {
      return new CreateProspectCommand(
          institutionName,
          category,
          locality,
          province,
          country,
          website,
          contactName,
          contactRole,
          email,
          phone,
          whatsapp,
          externalSourceId,
          source,
          evidence,
          estimatedStudents,
          priority,
          score,
          currentTools,
          administrativePain,
          verifiedAt,
          owner);
    }
  }
}
