package com.gestudio.crm.inbound;

import com.gestudio.crm.inbound.InboundAdminService.InboundView;
import com.gestudio.crm.inbound.InboundAdminService.WebhookHealth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inbound")
public class InboundAdminController {

  private final InboundAdminService adminService;
  private final QuarantineService quarantineService;

  public InboundAdminController(
      InboundAdminService adminService, QuarantineService quarantineService) {
    this.adminService = adminService;
    this.quarantineService = quarantineService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public Page<InboundView> list(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return adminService.list(status, page, size);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public InboundView detail(@PathVariable UUID id) {
    return adminService.detail(id);
  }

  @GetMapping("/webhook/health")
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public WebhookHealth webhookHealth() {
    return adminService.webhookHealth();
  }

  @PostMapping("/{id}/associate")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public InboundView associate(
      @PathVariable UUID id, @Valid @RequestBody AssociationRequest request) {
    quarantineService.associate(id, request.prospectId(), request.contactId());
    return adminService.detail(id);
  }

  @PostMapping("/{id}/retry-association")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public InboundView retry(@PathVariable UUID id) {
    quarantineService.retry(id);
    return adminService.detail(id);
  }

  @PostMapping("/{id}/discard")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public InboundView discard(@PathVariable UUID id, @Valid @RequestBody DiscardRequest request) {
    quarantineService.discard(id, request.reason());
    return adminService.detail(id);
  }

  public record AssociationRequest(@NotNull UUID prospectId, UUID contactId) {}

  public record DiscardRequest(@NotBlank @Size(max = 500) String reason) {}
}
