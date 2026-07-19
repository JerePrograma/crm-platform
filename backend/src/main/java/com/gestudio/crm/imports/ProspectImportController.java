package com.gestudio.crm.imports;

import com.gestudio.crm.imports.ImportJobLifecycleService.ImportSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports/prospects")
@Tag(name = "Prospect imports")
public class ProspectImportController {

  private static final String EXECUTION_CONFIRMATION = "EXECUTE_PROSPECT_IMPORT";

  private final ProspectImportService prospectImportService;

  public ProspectImportController(ProspectImportService prospectImportService) {
    this.prospectImportService = prospectImportService;
  }

  @PostMapping(path = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Validate and preview a prospect import without domain writes")
  public ImportSummary preview(@RequestPart("file") MultipartFile file) {
    return prospectImportService.importFile(file.getOriginalFilename(), bytes(file), true);
  }

  @PostMapping(path = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Execute a confirmed prospect and exclusion import")
  public ImportSummary execute(
      @RequestPart("file") MultipartFile file,
      @RequestHeader("X-Import-Confirmation") String confirmation) {
    if (!EXECUTION_CONFIRMATION.equals(confirmation)) {
      throw new IllegalArgumentException(
          "X-Import-Confirmation must equal " + EXECUTION_CONFIRMATION);
    }
    return prospectImportService.importFile(file.getOriginalFilename(), bytes(file), false);
  }

  @GetMapping("/{jobId}")
  @Operation(summary = "Get a persisted prospect import summary")
  public ImportSummary get(@PathVariable UUID jobId) {
    return prospectImportService.getSummary(jobId);
  }

  private byte[] bytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException exception) {
      throw new IllegalArgumentException("Import file could not be read", exception);
    }
  }
}
