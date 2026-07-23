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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProspectImportService {

  private static final int MAX_IMPORT_BYTES = 10 * 1024 * 1024;
  private static final int MAX_FILE_NAME_LENGTH = 255;

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
    String safeFileName = safeFileName(fileName);
    validateFile(safeFileName, bytes);
    SourceType sourceType = sourceType(safeFileName);
    String sha256 = sha256(bytes);
    String idempotencyKey = "prospects:" + sha256 + ":dryRun=" + dryRun;
    StartResult start =
        lifecycleService.start(safeFileName, sha256, idempotencyKey, sourceType, dryRun);
    if (start.existing()) {
      return start.summary();
    }

    UUID jobId = start.jobId();
    MutableCounters counters = new MutableCounters();
    try {
      ParsedImport parsed = parser.parse(safeFileName, bytes);
      Set<String> fileExclusionEmails = fileExclusionEmails(parsed);
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
          counters.add(
              rowProcessor.processProspect(
                  jobId,
                  prospect,
                  dryRun,
                  isExcludedByImportFile(prospect, fileExclusionEmails)));
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

  static Set<String> fileExclusionEmails(ParsedImport parsed) {
    return parsed.exclusions().stream()
        .map(ExclusionCandidate::email)
        .map(ProspectImportService::normalizedEmailKey)
        .filter(value -> value != null)
        .collect(Collectors.toUnmodifiableSet());
  }

  static boolean isExcludedByImportFile(
      ProspectCandidate prospect, Set<String> fileExclusionEmails) {
    String email = normalizedEmailKey(prospect.email());
    return email != null && fileExclusionEmails.contains(email);
  }

  private static String normalizedEmailKey(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private String safeFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      throw new IllegalArgumentException("Import file name is required");
    }
    String normalizedPath = fileName.replace('\\', '/');
    String baseName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1).trim();
    baseName = baseName.replaceAll("[\\p{Cntrl}]", "_");
    if (baseName.isBlank()) {
      throw new IllegalArgumentException("Import file name is required");
    }
    if (baseName.length() <= MAX_FILE_NAME_LENGTH) {
      return baseName;
    }
    int extensionIndex = baseName.lastIndexOf('.');
    String extension = extensionIndex < 0 ? "" : baseName.substring(extensionIndex);
    int stemLength = MAX_FILE_NAME_LENGTH - extension.length();
    if (stemLength <= 0) {
      throw new IllegalArgumentException("Import file name extension is too long");
    }
    return baseName.substring(0, stemLength) + extension;
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
