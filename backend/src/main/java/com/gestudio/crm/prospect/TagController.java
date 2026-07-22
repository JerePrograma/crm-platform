package com.gestudio.crm.prospect;

import com.gestudio.crm.prospect.TagService.TagView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

  private final TagService service;

  public TagController(TagService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  public List<TagView> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
    return service.list(includeInactive);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public TagView create(@Valid @RequestBody CreateRequest request) {
    return service.create(request.name(), request.color());
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public TagView update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest request) {
    return service.update(id, request.version(), request.name(), request.color(), request.active());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public TagView deactivate(@PathVariable UUID id, @RequestParam @PositiveOrZero long version) {
    return service.deactivate(id, version);
  }

  @GetMapping("/prospects/{prospectId}")
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  public List<TagView> forProspect(@PathVariable UUID prospectId) {
    return service.forProspect(prospectId);
  }

  @PostMapping("/{id}/assign")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public void assign(@PathVariable UUID id, @Valid @RequestBody AssignRequest request) {
    service.assign(id, request.prospectIds());
  }

  @DeleteMapping("/{id}/prospects/{prospectId}")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public void unassign(@PathVariable UUID id, @PathVariable UUID prospectId) {
    service.unassign(id, prospectId);
  }

  public record CreateRequest(@NotBlank @Size(max = 80) String name, @NotBlank String color) {}

  public record UpdateRequest(
      @PositiveOrZero long version,
      @NotBlank @Size(max = 80) String name,
      @NotBlank String color,
      boolean active) {}

  public record AssignRequest(@NotEmpty @Size(max = 200) List<UUID> prospectIds) {}
}
