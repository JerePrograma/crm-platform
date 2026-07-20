package com.gestudio.crm.imports;

import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.imports.DuplicateReview.MatchType;
import com.gestudio.crm.imports.DuplicateReview.Status;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportOperationsQueryService {

  private final ImportJobRepository importJobRepository;
  private final ImportRowRepository importRowRepository;
  private final DuplicateReviewRepository duplicateReviewRepository;

  public ImportOperationsQueryService(
      ImportJobRepository importJobRepository,
      ImportRowRepository importRowRepository,
      DuplicateReviewRepository duplicateReviewRepository) {
    this.importJobRepository = importJobRepository;
    this.importRowRepository = importRowRepository;
    this.duplicateReviewRepository = duplicateReviewRepository;
  }

  @Transactional(readOnly = true)
  public List<ImportRowView> rows(UUID jobId) {
    if (!importJobRepository.existsById(jobId)) {
      throw new ResourceNotFoundException("Import job not found: " + jobId);
    }
    return importRowRepository
        .findAllByImportJobIdOrderBySourceSheetAscRowNumberAsc(jobId)
        .stream()
        .map(
            row ->
                new ImportRowView(
                    row.getId(),
                    row.getSourceSheet(),
                    row.getRowNumber(),
                    row.getStatus(),
                    row.getNormalizedEmail(),
                    row.getNormalizedPhone(),
                    row.getErrorMessage(),
                    row.getProspect() == null ? null : row.getProspect().getId()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DuplicateReviewView> pendingReviews() {
    return duplicateReviewRepository.findAllByStatusOrderByCreatedAtAsc(Status.PENDING).stream()
        .map(
            review ->
                new DuplicateReviewView(
                    review.getId(),
                    review.getImportRow().getId(),
                    review.getImportRow().getSourceSheet(),
                    review.getImportRow().getRowNumber(),
                    review.getMatchType(),
                    review.getConfidence(),
                    review.getExistingProspect() == null
                        ? null
                        : review.getExistingProspect().getId(),
                    review.getStatus(),
                    review.getNotes(),
                    review.getCreatedAt()))
        .toList();
  }

  public record ImportRowView(
      UUID id,
      String sourceSheet,
      int rowNumber,
      ImportRow.Status status,
      String normalizedEmail,
      String normalizedPhone,
      String errorMessage,
      UUID prospectId) {}

  public record DuplicateReviewView(
      UUID id,
      UUID importRowId,
      String sourceSheet,
      int rowNumber,
      MatchType matchType,
      BigDecimal confidence,
      UUID existingProspectId,
      Status status,
      String notes,
      java.time.Instant createdAt) {}
}
