package com.gestudio.crm.imports;

import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.imports.ImportJob.SourceType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportJobLifecycleService {

  private final ImportJobRepository importJobRepository;

  public ImportJobLifecycleService(ImportJobRepository importJobRepository) {
    this.importJobRepository = importJobRepository;
  }

  @Transactional
  public StartResult start(
      String fileName,
      String fileSha256,
      String idempotencyKey,
      SourceType sourceType,
      boolean dryRun) {
    Optional<ImportJob> existing = importJobRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
      return new StartResult(existing.get().getId(), true, toSummary(existing.get()));
    }
    ImportJob job =
        importJobRepository.save(
            ImportJob.create(fileName, fileSha256, idempotencyKey, sourceType, dryRun));
    job.start();
    return new StartResult(job.getId(), false, toSummary(job));
  }

  @Transactional
  public ImportSummary complete(UUID jobId, ImportCounters counters) {
    ImportJob job = get(jobId);
    job.complete(
        counters.totalRows(),
        counters.acceptedRows(),
        counters.rejectedRows(),
        counters.duplicateRows(),
        counters.reviewRows());
    return toSummary(job);
  }

  @Transactional
  public void fail(UUID jobId, String message) {
    ImportJob job = get(jobId);
    job.fail(message == null ? "Unexpected import failure" : message);
  }

  @Transactional(readOnly = true)
  public ImportSummary getSummary(UUID jobId) {
    return toSummary(get(jobId));
  }

  private ImportJob get(UUID jobId) {
    return importJobRepository
        .findById(jobId)
        .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));
  }

  private ImportSummary toSummary(ImportJob job) {
    return new ImportSummary(
        job.getId(),
        job.getFileName(),
        job.getFileSha256(),
        job.getSourceType(),
        job.isDryRun(),
        job.getStatus(),
        job.getTotalRows(),
        job.getAcceptedRows(),
        job.getRejectedRows(),
        job.getDuplicateRows(),
        job.getReviewRows(),
        job.getErrorMessage(),
        job.getStartedAt(),
        job.getCompletedAt());
  }

  public record StartResult(UUID jobId, boolean existing, ImportSummary summary) {}

  public record ImportCounters(
      int totalRows, int acceptedRows, int rejectedRows, int duplicateRows, int reviewRows) {}

  public record ImportSummary(
      UUID id,
      String fileName,
      String fileSha256,
      SourceType sourceType,
      boolean dryRun,
      ImportJob.Status status,
      int totalRows,
      int acceptedRows,
      int rejectedRows,
      int duplicateRows,
      int reviewRows,
      String errorMessage,
      java.time.Instant startedAt,
      java.time.Instant completedAt) {}
}
