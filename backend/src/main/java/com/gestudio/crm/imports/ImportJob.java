package com.gestudio.crm.imports;

import com.gestudio.crm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "import_job")
public class ImportJob extends BaseEntity {

  @Column(nullable = false)
  private String fileName;

  @Column(nullable = false, length = 64)
  private String fileSha256;

  @Column(nullable = false, unique = true)
  private String idempotencyKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SourceType sourceType;

  @Column(nullable = false)
  private boolean dryRun;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  @Column(nullable = false)
  private int totalRows;

  @Column(nullable = false)
  private int acceptedRows;

  @Column(nullable = false)
  private int excludedRows;

  @Column(nullable = false)
  private int rejectedRows;

  @Column(nullable = false)
  private int duplicateRows;

  @Column(nullable = false)
  private int reviewRows;

  private String errorMessage;
  private Instant startedAt;
  private Instant completedAt;

  protected ImportJob() {}

  private ImportJob(
      String fileName,
      String fileSha256,
      String idempotencyKey,
      SourceType sourceType,
      boolean dryRun) {
    this.fileName = fileName;
    this.fileSha256 = fileSha256;
    this.idempotencyKey = idempotencyKey;
    this.sourceType = sourceType;
    this.dryRun = dryRun;
    this.status = Status.PENDING;
  }

  public static ImportJob create(
      String fileName,
      String fileSha256,
      String idempotencyKey,
      SourceType sourceType,
      boolean dryRun) {
    if (fileName == null || fileName.isBlank()) {
      throw new IllegalArgumentException("Import file name is required");
    }
    if (fileSha256 == null || fileSha256.length() != 64) {
      throw new IllegalArgumentException("Import SHA-256 must contain 64 hexadecimal characters");
    }
    if (idempotencyKey == null || idempotencyKey.isBlank() || sourceType == null) {
      throw new IllegalArgumentException("Import idempotency key and source type are required");
    }
    return new ImportJob(fileName, fileSha256, idempotencyKey, sourceType, dryRun);
  }

  public void start() {
    status = Status.RUNNING;
    startedAt = Instant.now();
    errorMessage = null;
  }

  public void complete(
      int totalRows,
      int acceptedRows,
      int excludedRows,
      int rejectedRows,
      int duplicateRows,
      int reviewRows) {
    this.totalRows = totalRows;
    this.acceptedRows = acceptedRows;
    this.excludedRows = excludedRows;
    this.rejectedRows = rejectedRows;
    this.duplicateRows = duplicateRows;
    this.reviewRows = reviewRows;
    status = Status.COMPLETED;
    completedAt = Instant.now();
  }

  public void fail(String message) {
    status = Status.FAILED;
    errorMessage = message;
    completedAt = Instant.now();
  }

  public String getFileName() {
    return fileName;
  }

  public String getFileSha256() {
    return fileSha256;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public SourceType getSourceType() {
    return sourceType;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public Status getStatus() {
    return status;
  }

  public int getTotalRows() {
    return totalRows;
  }

  public int getAcceptedRows() {
    return acceptedRows;
  }

  public int getExcludedRows() {
    return excludedRows;
  }

  public int getRejectedRows() {
    return rejectedRows;
  }

  public int getDuplicateRows() {
    return duplicateRows;
  }

  public int getReviewRows() {
    return reviewRows;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public enum SourceType {
    CSV,
    XLSX
  }

  public enum Status {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
  }
}
