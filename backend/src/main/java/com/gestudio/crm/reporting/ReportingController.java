package com.gestudio.crm.reporting;

import com.gestudio.crm.reporting.ReportingService.DashboardReport;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

  private final ReportingService service;

  public ReportingController(ReportingService service) {
    this.service = service;
  }

  @GetMapping("/dashboard")
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public DashboardReport dashboard(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return service.dashboard(from, to);
  }

  @GetMapping(value = "/dashboard.csv", produces = "text/csv")
  @PreAuthorize("hasAuthority('REPORT_READ')")
  public ResponseEntity<String> csv(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=crm-dashboard.csv")
        .body(service.csv(from, to));
  }
}
