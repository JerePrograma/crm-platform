package com.gestudio.crm.imports;

import com.gestudio.crm.common.BaseEntity;
import com.gestudio.crm.prospect.Prospect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "duplicate_review")
public class DuplicateReview extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "import_row_id", nullable = false, unique = true)
  private ImportRow importRow;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "existing_prospect_id")
  private Prospect existingProspect;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MatchType matchType;

  @Column(nullable = false, precision = 5, scale = 4)
  private BigDecimal confidence;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  private String notes;

  protected DuplicateReview() {}

  private DuplicateReview(
      ImportRow importRow,
      Prospect existingProspect,
      MatchType matchType,
      BigDecimal confidence,
      String notes) {
    this.importRow = importRow;
    this.existingProspect = existingProspect;
    this.matchType = matchType;
    this.confidence = confidence;
    this.status = Status.PENDING;
    this.notes = notes;
  }

  public static DuplicateReview create(
      ImportRow importRow,
      Prospect existingProspect,
      MatchType matchType,
      BigDecimal confidence,
      String notes) {
    if (importRow == null || matchType == null || confidence == null) {
      throw new IllegalArgumentException("Import row, match type and confidence are required");
    }
    if (confidence.compareTo(BigDecimal.ZERO) < 0
        || confidence.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("Duplicate confidence must be between zero and one");
    }
    return new DuplicateReview(importRow, existingProspect, matchType, confidence, notes);
  }

  public ImportRow getImportRow() {
    return importRow;
  }

  public Prospect getExistingProspect() {
    return existingProspect;
  }

  public MatchType getMatchType() {
    return matchType;
  }

  public BigDecimal getConfidence() {
    return confidence;
  }

  public Status getStatus() {
    return status;
  }

  public String getNotes() {
    return notes;
  }

  public enum MatchType {
    EMAIL,
    PHONE,
    DOMAIN,
    NAME_LOCATION,
    NOMINAL_SIMILARITY
  }

  public enum Status {
    PENDING,
    MERGED,
    IGNORED,
    REJECTED
  }
}
