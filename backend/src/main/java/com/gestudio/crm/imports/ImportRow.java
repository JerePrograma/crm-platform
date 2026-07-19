package com.gestudio.crm.imports;

import com.gestudio.crm.common.BaseEntity;
import com.gestudio.crm.prospect.Prospect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "import_row")
public class ImportRow extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "import_job_id", nullable = false)
  private ImportJob importJob;

  @Column(nullable = false)
  private int rowNumber;

  @Column(nullable = false)
  private String rawData;

  private String normalizedEmail;
  private String normalizedPhone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  private String errorMessage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prospect_id")
  private Prospect prospect;

  protected ImportRow() {}

  private ImportRow(
      ImportJob importJob,
      int rowNumber,
      String rawData,
      String normalizedEmail,
      String normalizedPhone) {
    this.importJob = importJob;
    this.rowNumber = rowNumber;
    this.rawData = rawData;
    this.normalizedEmail = normalizedEmail;
    this.normalizedPhone = normalizedPhone;
    this.status = Status.PENDING;
  }

  public static ImportRow create(
      ImportJob importJob,
      int rowNumber,
      String rawData,
      String normalizedEmail,
      String normalizedPhone) {
    if (importJob == null || rowNumber <= 0 || rawData == null) {
      throw new IllegalArgumentException("Import job, positive row number and raw data are required");
    }
    return new ImportRow(importJob, rowNumber, rawData, normalizedEmail, normalizedPhone);
  }

  public void accept(Prospect prospect) {
    this.prospect = prospect;
    status = Status.ACCEPTED;
    errorMessage = null;
  }

  public void exclude(Prospect prospect) {
    this.prospect = prospect;
    status = Status.EXCLUDED;
    errorMessage = null;
  }

  public void reject(String message) {
    status = Status.REJECTED;
    errorMessage = message;
  }

  public void markDuplicate() {
    status = Status.DUPLICATE;
    errorMessage = null;
  }

  public void requireReview() {
    status = Status.REVIEW_REQUIRED;
    errorMessage = null;
  }

  public ImportJob getImportJob() {
    return importJob;
  }

  public int getRowNumber() {
    return rowNumber;
  }

  public String getRawData() {
    return rawData;
  }

  public String getNormalizedEmail() {
    return normalizedEmail;
  }

  public String getNormalizedPhone() {
    return normalizedPhone;
  }

  public Status getStatus() {
    return status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Prospect getProspect() {
    return prospect;
  }

  public enum Status {
    PENDING,
    ACCEPTED,
    EXCLUDED,
    REJECTED,
    DUPLICATE,
    REVIEW_REQUIRED
  }
}
