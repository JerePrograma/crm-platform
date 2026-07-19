package com.gestudio.crm.contact;

import com.gestudio.crm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "contact_channel")
public class ContactChannel extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "contact_id", nullable = false)
  private Contact contact;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContactChannelType type;

  private String value;

  @Column(nullable = false)
  private String normalizedValue;

  @Column(nullable = false)
  private boolean primaryChannel;

  protected ContactChannel() {}

  private ContactChannel(
      Contact contact,
      ContactChannelType type,
      String value,
      String normalizedValue,
      boolean primaryChannel) {
    this.contact = contact;
    this.type = type;
    this.value = value == null ? null : value.trim();
    this.normalizedValue = normalizedValue;
    this.primaryChannel = primaryChannel;
  }

  public static ContactChannel create(
      Contact contact,
      ContactChannelType type,
      String value,
      String normalizedValue,
      boolean primaryChannel) {
    if (contact == null || type == null) {
      throw new IllegalArgumentException("Contact and channel type are required");
    }
    if (normalizedValue == null || normalizedValue.isBlank()) {
      throw new IllegalArgumentException("Normalized channel value is required");
    }
    return new ContactChannel(contact, type, value, normalizedValue, primaryChannel);
  }

  public Contact getContact() {
    return contact;
  }

  public ContactChannelType getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public String getNormalizedValue() {
    return normalizedValue;
  }

  public boolean isPrimaryChannel() {
    return primaryChannel;
  }
}
