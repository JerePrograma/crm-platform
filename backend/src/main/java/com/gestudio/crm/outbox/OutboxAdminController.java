package com.gestudio.crm.outbox;

import com.gestudio.crm.outbox.OutboxAdminService.OutboxView;
import com.gestudio.crm.outbox.OutboxAdminService.StatusMetric;
import com.gestudio.crm.outbox.OutboxWorkerService.RunResult;
import com.gestudio.crm.outbox.OutboxWorkerService.WorkerHealth;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outbox")
public class OutboxAdminController {

  private final OutboxAdminService adminService;
  private final OutboxWorkerService workerService;

  public OutboxAdminController(OutboxAdminService adminService, OutboxWorkerService workerService) {
    this.adminService = adminService;
    this.workerService = workerService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public Page<OutboxView> list(
      @RequestParam(required = false) OutboxStatus status,
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) String aggregateType,
      @RequestParam(required = false) UUID aggregateId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(defaultValue = "createdAt,desc") String sort) {
    return adminService.list(
        status, eventType, aggregateType, aggregateId, from, to, page, size, sort);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public OutboxView detail(@PathVariable UUID id) {
    return adminService.detail(id);
  }

  @GetMapping("/metrics")
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public List<StatusMetric> metrics() {
    return adminService.metrics();
  }

  @PostMapping("/{id}/requeue")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public OutboxView requeue(@PathVariable UUID id) {
    return adminService.requeue(id);
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public OutboxView cancel(@PathVariable UUID id) {
    return adminService.cancel(id);
  }

  @GetMapping("/worker/health")
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public WorkerState workerHealth() {
    return new WorkerState(workerService.health(), adminService.paused());
  }

  @PostMapping("/worker/run-once")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public RunResult runOnce() {
    return workerService.runOnce();
  }

  @PostMapping("/worker/pause")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public WorkerState pause() {
    adminService.pause(true);
    return workerHealth();
  }

  @PostMapping("/worker/resume")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public WorkerState resume() {
    adminService.pause(false);
    return workerHealth();
  }

  public record WorkerState(WorkerHealth worker, boolean tenantPaused) {}
}
