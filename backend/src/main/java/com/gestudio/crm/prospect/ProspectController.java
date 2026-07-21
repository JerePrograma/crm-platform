package com.gestudio.crm.prospect;

import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.ProspectOperationsService.OperationalProspectView;
import com.gestudio.crm.prospect.ProspectOperationsService.PageResult;
import com.gestudio.crm.prospect.ProspectOperationsService.SearchFilter;
import com.gestudio.crm.prospect.ProspectOperationsService.TransitionCommand;
import com.gestudio.crm.prospect.ProspectOperationsService.UpdateProspectCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/prospects")
@Tag(name = "Prospects")
public class ProspectController {

  private final ProspectApplicationService prospectApplicationService;
  private final ProspectOperationsService prospectOperationsService;

  public ProspectController(
      ProspectApplicationService prospectApplicationService,
      ProspectOperationsService prospectOperationsService) {
    this.prospectApplicationService = prospectApplicationService;
    this.prospectOperationsService = prospectOperationsService;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  @Operation(summary = "Create a prospect after normalization and exclusion checks")
  public ResponseEntity<OperationalProspectView> create(
      @Valid @RequestBody CreateProspectRequest request) {
    UUID id = prospectApplicationService.create(request.toCommand()).id();
    return ResponseEntity.created(URI.create("/api/v1/prospects/" + id))
        .body(prospectOperationsService.get(id));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  @Operation(summary = "Get the integral prospect summary")
  public OperationalProspectView get(@PathVariable UUID id) {
    return prospectOperationsService.get(id);
  }

  @GetMapping
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  @Operation(summary = "List prospects with pagination and optional status filtering")
  public PageResult<OperationalProspectView> list(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) ProspectStatus status,
      @RequestParam(required = false) UUID ownerUserId,
      @RequestParam(defaultValue = "false") boolean archived,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(defaultValue = "createdAt,desc") String sort) {
    String[] sorting = sort.split(",", 2);
    return prospectOperationsService.search(
        new SearchFilter(
            query,
            status,
            ownerUserId,
            archived,
            page,
            size,
            sorting[0],
            sorting.length == 2 ? sorting[1] : "desc"));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public OperationalProspectView update(
      @PathVariable UUID id, @Valid @RequestBody UpdateProspectRequest request) {
    return prospectOperationsService.update(id, request.toCommand());
  }

  @PostMapping("/{id}/archive")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public OperationalProspectView archive(
      @PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
    return prospectOperationsService.archive(id, request.version());
  }

  @PostMapping("/{id}/restore")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public OperationalProspectView restore(
      @PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
    return prospectOperationsService.restore(id, request.version());
  }

  @PostMapping("/{id}/transitions")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public OperationalProspectView transition(
      @PathVariable UUID id, @Valid @RequestBody TransitionRequest request) {
    return prospectOperationsService.transition(id, request.toCommand());
  }

  @GetMapping(value = "/export", produces = "text/csv")
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public ResponseEntity<String> export(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) ProspectStatus status,
      @RequestParam(required = false) UUID ownerUserId,
      @RequestParam(defaultValue = "false") boolean archived,
      @RequestParam(defaultValue = "createdAt,desc") String sort) {
    String[] sorting = sort.split(",", 2);
    String csv =
        prospectOperationsService.exportCsv(
            new SearchFilter(
                query,
                status,
                ownerUserId,
                archived,
                0,
                10000,
                sorting[0],
                sorting.length == 2 ? sorting[1] : "desc"));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prospects.csv")
        .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
        .body(csv);
  }

  public record UpdateProspectRequest(
      @PositiveOrZero long version,
      @NotBlank String displayName,
      String legalName,
      Integer priority,
      Integer score,
      Integer estimatedStudents,
      String source,
      String sourceDetail,
      UUID ownerUserId,
      String website,
      String address,
      String city,
      String province,
      String country,
      String timezone,
      String notesSummary,
      Instant nextActionAt) {
    UpdateProspectCommand toCommand() {
      return new UpdateProspectCommand(
          version,
          displayName,
          legalName,
          priority,
          score,
          estimatedStudents,
          source,
          sourceDetail,
          ownerUserId,
          website,
          address,
          city,
          province,
          country,
          timezone,
          notesSummary,
          nextActionAt);
    }
  }

  public record VersionRequest(@PositiveOrZero long version) {}

  public record TransitionRequest(
      @PositiveOrZero long version,
      @NotNull ProspectStatus status,
      String reason,
      String comment,
      Instant scheduledAt,
      boolean proposalException) {
    TransitionCommand toCommand() {
      return new TransitionCommand(
          version, status, reason, comment, scheduledAt, proposalException);
    }
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
