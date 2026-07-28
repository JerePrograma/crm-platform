package com.gestudio.crm.imports;

import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.imports.DuplicateReview.MatchType;
import com.gestudio.crm.imports.DuplicateReview.Status;
import com.gestudio.crm.security.CurrentActor;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportOperationsQueryService {

  private static final int DEFAULT_PAGE_SIZE = 25;
  private static final int MAX_PAGE_SIZE = 100;
  private static final int MAX_QUERY_LENGTH = 200;
  private static final int MAX_SOURCE_SHEET_LENGTH = 200;

  private final ImportJobRepository importJobRepository;
  private final ImportRowRepository importRowRepository;
  private final DuplicateReviewRepository duplicateReviewRepository;
  private final CurrentActor currentActor;

  public ImportOperationsQueryService(
      ImportJobRepository importJobRepository,
      ImportRowRepository importRowRepository,
      DuplicateReviewRepository duplicateReviewRepository,
      CurrentActor currentActor) {
    this.importJobRepository = importJobRepository;
    this.importRowRepository = importRowRepository;
    this.duplicateReviewRepository = duplicateReviewRepository;
    this.currentActor = currentActor;
  }

  @Transactional(readOnly = true)
  public ImportRowPage rows(UUID jobId, RowSearchFilter filter) {
    UUID organizationId = currentActor.organizationId();
    if (importJobRepository.findByIdAndOrganizationId(jobId, organizationId).isEmpty()) {
      throw new ResourceNotFoundException("Import job not found: " + jobId);
    }

    RowSearchFilter normalized = normalize(filter);
    Specification<ImportRow> specification =
        (root, criteriaQuery, builder) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(builder.equal(root.<UUID>get("organizationId"), organizationId));
          predicates.add(builder.equal(root.get("importJob").<UUID>get("id"), jobId));

          if (normalized.status() != null) {
            predicates.add(
                builder.equal(root.<ImportRow.Status>get("status"), normalized.status()));
          }
          if (normalized.sourceSheet() != null) {
            predicates.add(
                builder.equal(root.<String>get("sourceSheet"), normalized.sourceSheet()));
          }
          if (normalized.query() != null) {
            String pattern = containsPattern(normalized.query());
            List<Predicate> searchPredicates = new ArrayList<>();
            searchPredicates.add(
                builder.like(builder.lower(root.<String>get("sourceSheet")), pattern, '\\'));
            searchPredicates.add(
                builder.like(builder.lower(root.<String>get("normalizedEmail")), pattern, '\\'));
            searchPredicates.add(
                builder.like(builder.lower(root.<String>get("normalizedPhone")), pattern, '\\'));
            searchPredicates.add(
                builder.like(builder.lower(root.<String>get("errorMessage")), pattern, '\\'));

            Integer rowNumber = parseRowNumber(normalized.query());
            if (rowNumber != null) {
              searchPredicates.add(builder.equal(root.<Integer>get("rowNumber"), rowNumber));
            }
            predicates.add(builder.or(searchPredicates.toArray(Predicate[]::new)));
          }

          return builder.and(predicates.toArray(Predicate[]::new));
        };

    Sort sort =
        Sort.by(Sort.Order.asc("sourceSheet"), Sort.Order.asc("rowNumber"), Sort.Order.asc("id"));
    Page<ImportRow> result =
        importRowRepository.findAll(
            specification, PageRequest.of(normalized.page(), normalized.size(), sort));

    List<ImportRowView> content = result.getContent().stream().map(this::view).toList();
    List<String> sourceSheets = importRowRepository.findDistinctSourceSheets(organizationId, jobId);

    return new ImportRowPage(
        content,
        result.getTotalElements(),
        result.getTotalPages(),
        result.getNumber(),
        result.getSize(),
        result.isFirst(),
        result.isLast(),
        sourceSheets);
  }

  @Transactional(readOnly = true)
  public List<DuplicateReviewView> pendingReviews() {
    return duplicateReviewRepository
        .findAllByOrganizationIdAndStatusOrderByCreatedAtAsc(
            currentActor.organizationId(), Status.PENDING)
        .stream()
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

  private RowSearchFilter normalize(RowSearchFilter filter) {
    RowSearchFilter source =
        filter == null ? new RowSearchFilter(null, null, null, 0, DEFAULT_PAGE_SIZE) : filter;
    int page = Math.max(0, source.page());
    int size = Math.min(Math.max(source.size(), 1), MAX_PAGE_SIZE);
    String sourceSheet =
        normalizeText(source.sourceSheet(), MAX_SOURCE_SHEET_LENGTH, "Source sheet");
    String query = normalizeText(source.query(), MAX_QUERY_LENGTH, "Search query");
    return new RowSearchFilter(source.status(), sourceSheet, query, page, size);
  }

  private String normalizeText(String value, int maximumLength, String label) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() > maximumLength) {
      throw new IllegalArgumentException(
          label + " must contain at most " + maximumLength + " characters");
    }
    return normalized;
  }

  private String containsPattern(String value) {
    String escaped =
        value
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return "%" + escaped + "%";
  }

  private Integer parseRowNumber(String value) {
    try {
      int parsed = Integer.parseInt(value);
      return parsed > 0 ? parsed : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private ImportRowView view(ImportRow row) {
    return new ImportRowView(
        row.getId(),
        row.getSourceSheet(),
        row.getRowNumber(),
        row.getStatus(),
        row.getNormalizedEmail(),
        row.getNormalizedPhone(),
        row.getErrorMessage(),
        row.getProspect() == null ? null : row.getProspect().getId());
  }

  public record RowSearchFilter(
      ImportRow.Status status, String sourceSheet, String query, int page, int size) {}

  public record ImportRowPage(
      List<ImportRowView> content,
      long totalElements,
      int totalPages,
      int number,
      int size,
      boolean first,
      boolean last,
      List<String> sourceSheets) {}

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
