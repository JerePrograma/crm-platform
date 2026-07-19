package com.gestudio.crm.exclusion;

import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.exclusion.ExclusionApplicationService.ExclusionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exclusions")
@Tag(name = "Exclusions")
public class ExclusionController {

  private final ExclusionApplicationService exclusionApplicationService;

  public ExclusionController(ExclusionApplicationService exclusionApplicationService) {
    this.exclusionApplicationService = exclusionApplicationService;
  }

  @PostMapping
  @Operation(summary = "Create a dominant contact exclusion")
  public ResponseEntity<ExclusionView> create(@Valid @RequestBody CreateExclusionRequest request) {
    ExclusionView created =
        exclusionApplicationService.create(
            request.channelType(), request.value(), request.reason());
    return ResponseEntity.created(URI.create("/api/v1/exclusions/" + created.id())).body(created);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an exclusion")
  public ExclusionView get(@PathVariable UUID id) {
    return exclusionApplicationService.get(id);
  }

  @GetMapping
  @Operation(summary = "List exclusions")
  public Page<ExclusionView> list(@PageableDefault(size = 50) Pageable pageable) {
    return exclusionApplicationService.list(pageable);
  }

  public record CreateExclusionRequest(
      @NotNull ContactChannelType channelType,
      @NotBlank String value,
      @NotNull ExclusionReason reason) {}
}
