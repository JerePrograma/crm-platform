package com.gestudio.crm.audit;

import com.gestudio.crm.audit.AuditQueryService.AuditEventView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit")
@PreAuthorize("hasAuthority('AUDIT_READ')")
public class AuditController {

  private final AuditQueryService auditQueryService;

  public AuditController(AuditQueryService auditQueryService) {
    this.auditQueryService = auditQueryService;
  }

  @GetMapping
  @Operation(summary = "List recent commercial audit events")
  public List<AuditEventView> recent(@RequestParam(defaultValue = "100") int limit) {
    return auditQueryService.recent(limit);
  }
}
