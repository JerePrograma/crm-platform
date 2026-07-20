package com.gestudio.crm.imports;

import com.gestudio.crm.imports.ImportJob.SourceType;
import com.gestudio.crm.imports.ImportJobLifecycleService.ImportCounters;
import com.gestudio.crm.imports.ImportJobLifecycleService.ImportSummary;
import com.gestudio.crm.imports.ImportJobLifecycleService.RowOutcome;
import com.gestudio.crm.imports.ImportJobLifecycleService.StartResult;
import com.gestudio.crm.imports.ProspectImportFileParser.ExclusionCandidate;
import com.gestudio.crm.imports.ProspectImportFileParser.ParsedImport;
import com.gestudio.crm.imports.ProspectImportFileParser.ProspectCandidate;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProspectImportService {

  private static final int MAX_IMPORT_BYTES = 10 * 1024 * 1024;

  private final ProspectImportFileParser parser;
  private final ProspectImportRowProcessor rowProcessor;
  private final ImportJobLifecycleService lifecycleService;

  public ProspectImportService(
      ProspectImportFileParser parser,
      ProspectImportRowProcessor rowProcessor,
      ImportJobLifecycleService lifecycleService) {
    this.parser = parser;
    this.rowProcessor = rowProcessor;
    this.lifecycleService = lifecycleService;
  }

  public ImportSummary importFile(String fileName, byte[] bytes, boolean dryRun) {
    validateFile(fileName, bytes);
    SourceType sourceType = sourceType(fileName);
    String sha256 = sha256(bytes);
    String idempotencyKey = "prospects:" + sha256 + ":dryRun=" + dryRun;
    StartResult start =
        lifecycleService.start(fileName, sha256, idempotencyKey, sourceType, dryRun);
    if (start.existing()) {
      return start.summary();
    }

    UUID jobId = start.jobId();
    MutableCounters counters = new MutableCounters();
    try {
      ParsedImport parsed = parser.parse(fileName, bytes);
      for (ExclusionCandidate exclusion : parsed.exclusions()) {
        counters.total++;
        try {
          counters.add(rowProcessor.processExclusion(jobId, exclusion, dryRun));
        } catch (RuntimeException exception) {
          rowProcessor.recordRejectedExclusion(jobId, exclusion, safeMessage(exception));
          counters.rejected++;
        }
      }
      for (ProspectCandidate prospect : parsed.prospects()) {
        counters.total++;
        try {
          counters.add(rowProcessor.processProspect(jobId, prospect, dryRun));
        } catch (RuntimeException exception) {
          rowProcessor.recordRejectedProspect(jobId, prospect, safeMessage(exception));
          counters.rejected++;
        }
      }
      return lifecycleService.complete(jobId, counters.toRecord());
    } catch (RuntimeException exception) {
      lifecycleService.fail(jobId, safeMessage(exception));
      throw exception;
    }
  }

  public ImportSummary getSummary(UUID jobId) {
    return lifecycleService.getSummary(jobId);
  }

  private void validateFile(String fileName, byte[] bytes) {
    if (fileName == null || fileName.isBlank()) {
      throw new IllegalArgumentException("Import file name is required");
    }
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("Import file is empty");
    }
    if (bytes.length > MAX_IMPORT_BYTES) {
      throw new IllegalArgumentException("Import file exceeds the 10 MB safety limit");
    }
  }

  private SourceType sourceType(String fileName) {
    String lowerCase = fileName.toLowerCase(Locale.ROOT);
    if (lowerCase.endsWith(".xlsx")) {
      return SourceType.XLSX;
    }
    if (lowerCase.endsWith(".csv")) {
      return SourceType.CSV;
    }
    throw new IllegalArgumentException("Only .xlsx and .csv prospect imports are supported");
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private String safeMessage(RuntimeException exception) {
    String message = exception.getMessage();
    return message == null || message.isBlank()
        ? exception.getClass().getSimpleName()
        : message.substring(0, Math.min(message.length(), 1000));
  }

  private static final class MutableCounters {
    private int total;
    private int accepted;
    private int excluded;
    private int rejected;
    private int duplicate;
    private int review;

    void add(RowOutcome outcome) {
      switch (outcome) {
        case ACCEPTED -> accepted++;
        case EXCLUDED -> excluded++;
        case REJECTED -> rejected++;
        case DUPLICATE -> duplicate++;
        case REVIEW_REQUIRED -> review++;
      }
    }

    ImportCounters toRecord() {
      return new ImportCounters(total, accepted, excluded, rejected, duplicate, review);
    }
  }
}
