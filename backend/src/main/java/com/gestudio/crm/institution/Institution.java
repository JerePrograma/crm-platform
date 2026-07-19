package com.gestudio.crm.institution;

import com.gestudio.crm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "institution")
public class Institution extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String normalizedName;

  private String category;
  private String locality;
  private String province;
  private String country;
  private String website;

  protected Institution() {}

  private Institution(
      String name,
      String normalizedName,
      String category,
      String locality,
      String province,
      String country,
      String website) {
    this.name = name;
    this.normalizedName = normalizedName;
    this.category = category;
    this.locality = locality;
    this.province = province;
    this.country = country;
    this.website = website;
  }

  public static Institution create(
      String name,
      String normalizedName,
      String category,
      String locality,
      String province,
      String country,
      String website) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Institution name is required");
    }
    if (normalizedName == null || normalizedName.isBlank()) {
      throw new IllegalArgumentException("Normalized institution name is required");
    }
    return new Institution(
        name.trim(), normalizedName, category, locality, province, country, website);
  }

  public String getName() {
    return name;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public String getCategory() {
    return category;
  }

  public String getLocality() {
    return locality;
  }

  public String getProvince() {
    return province;
  }

  public String getCountry() {
    return country;
  }

  public String getWebsite() {
    return website;
  }
}
