package com.gestudio.crm.exclusion;

import com.gestudio.crm.common.BaseEntity;
import com.gestudio.crm.contact.ContactChannelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "exclusion")
public class Exclusion extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContactChannelType channelType;

  @Column(nullable = false)
  private String normalizedValue;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ExclusionReason reason;

  protected Exclusion() {}

  private Exclusion(
      ContactChannelType channelType, String normalizedValue, ExclusionReason reason) {
    this.channelType = channelType;
    this.normalizedValue = normalizedValue;
    this.reason = reason;
  }

  public static Exclusion create(
      ContactChannelType channelType, String normalizedValue, ExclusionReason reason) {
    if (channelType == null || reason == null) {
      throw new IllegalArgumentException("Channel type and exclusion reason are required");
    }
    if (normalizedValue == null || normalizedValue.isBlank()) {
      throw new IllegalArgumentException("Normalized exclusion value is required");
    }
    return new Exclusion(channelType, normalizedValue, reason);
  }

  public ContactChannelType getChannelType() {
    return channelType;
  }

  public String getNormalizedValue() {
    return normalizedValue;
  }

  public ExclusionReason getReason() {
    return reason;
  }
}
