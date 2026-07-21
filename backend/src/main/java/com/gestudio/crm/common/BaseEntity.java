package com.gestudio.crm.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @Column(nullable = false, updatable = false)
  protected UUID id;

  @Column(nullable = false, updatable = false)
  protected UUID organizationId;

  @Version protected long version;

  @Column(nullable = false, updatable = false)
  protected Instant createdAt;

  @Column(nullable = false)
  protected Instant updatedAt;

  @PrePersist
  protected void initializeAuditFields() {
    Instant now = Instant.now();
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (organizationId == null) {
      organizationId = TenantIds.BOOTSTRAP_ORGANIZATION_ID;
    }
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  protected void touch() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public void assignOrganization(UUID organizationId) {
    if (organizationId == null) {
      throw new IllegalArgumentException("Organization is required");
    }
    if (this.organizationId != null && !this.organizationId.equals(organizationId)) {
      throw new IllegalStateException("An entity cannot move between organizations");
    }
    this.organizationId = organizationId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
