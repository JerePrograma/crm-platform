package com.gestudio.crm.contact;

import com.gestudio.crm.common.BaseEntity;
import com.gestudio.crm.institution.Institution;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "contact")
public class Contact extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  private String name;
  private String role;

  protected Contact() {}

  private Contact(Institution institution, String name, String role) {
    this.institution = institution;
    this.name = blankToNull(name);
    this.role = blankToNull(role);
  }

  public static Contact create(Institution institution, String name, String role) {
    if (institution == null) {
      throw new IllegalArgumentException("Institution is required");
    }
    return new Contact(institution, name, role);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public Institution getInstitution() {
    return institution;
  }

  public String getName() {
    return name;
  }

  public String getRole() {
    return role;
  }
}
