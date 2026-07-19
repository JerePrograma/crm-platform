package com.gestudio.crm.prospect;

import com.gestudio.crm.common.BaseEntity;
import com.gestudio.crm.institution.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "prospect")
public class Prospect extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  private String externalSourceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProspectStatus status;

  private Integer priority;
  private Integer score;
  private Integer estimatedStudents;
  private String currentTools;
  private String administrativePain;
  private String source;
  private String evidence;
  private Instant verifiedAt;
  private Instant lastContactAt;
  private Instant nextActionAt;
  private String owner;

  @Column(nullable = false)
  private boolean contactEligible;

  protected Prospect() {}

  private Prospect(
      Institution institution,
      String externalSourceId,
      ProspectStatus status,
      Integer priority,
      Integer score,
      Integer estimatedStudents,
      String currentTools,
      String administrativePain,
      String source,
      String evidence,
      Instant verifiedAt,
      String owner,
      boolean contactEligible) {
    this.institution = institution;
    this.externalSourceId = blankToNull(externalSourceId);
    this.status = status;
    this.priority = priority;
    this.score = score;
    this.estimatedStudents = estimatedStudents;
    this.currentTools = blankToNull(currentTools);
    this.administrativePain = blankToNull(administrativePain);
    this.source = blankToNull(source);
    this.evidence = blankToNull(evidence);
    this.verifiedAt = verifiedAt;
    this.owner = blankToNull(owner);
    this.contactEligible = contactEligible;
  }

  public static Prospect create(
      Institution institution,
      String externalSourceId,
      Integer priority,
      Integer score,
      Integer estimatedStudents,
      String currentTools,
      String administrativePain,
      String source,
      String evidence,
      Instant verifiedAt,
      String owner,
      boolean contactEligible) {
    if (institution == null) {
      throw new IllegalArgumentException("Institution is required");
    }
    return new Prospect(
        institution,
        externalSourceId,
        contactEligible ? ProspectStatus.NEW : ProspectStatus.DO_NOT_CONTACT,
        priority,
        score,
        estimatedStudents,
        currentTools,
        administrativePain,
        source,
        evidence,
        verifiedAt,
        owner,
        contactEligible);
  }

  public void markIneligible() {
    contactEligible = false;
    status = ProspectStatus.DO_NOT_CONTACT;
  }

  public void changeStatus(ProspectStatus newStatus) {
    if (newStatus == null) {
      throw new IllegalArgumentException("Prospect status is required");
    }
    status = newStatus;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public Institution getInstitution() {
    return institution;
  }

  public String getExternalSourceId() {
    return externalSourceId;
  }

  public ProspectStatus getStatus() {
    return status;
  }

  public Integer getPriority() {
    return priority;
  }

  public Integer getScore() {
    return score;
  }

  public Integer getEstimatedStudents() {
    return estimatedStudents;
  }

  public String getCurrentTools() {
    return currentTools;
  }

  public String getAdministrativePain() {
    return administrativePain;
  }

  public String getSource() {
    return source;
  }

  public String getEvidence() {
    return evidence;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public Instant getLastContactAt() {
    return lastContactAt;
  }

  public Instant getNextActionAt() {
    return nextActionAt;
  }

  public String getOwner() {
    return owner;
  }

  public boolean isContactEligible() {
    return contactEligible;
  }
}
