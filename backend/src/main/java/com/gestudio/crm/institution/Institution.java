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
  private String normalizedLocality;
  private String province;
  private String country;
  private String website;
  private String websiteDomain;

  protected Institution() {}

  private Institution(
      String name,
      String normalizedName,
      String category,
      String locality,
      String normalizedLocality,
      String province,
      String country,
      String website,
      String websiteDomain) {
    this.name = name;
    this.normalizedName = normalizedName;
    this.category = category;
    this.locality = locality;
    this.normalizedLocality = normalizedLocality;
    this.province = province;
    this.country = country;
    this.website = website;
    this.websiteDomain = websiteDomain;
  }

  public static Institution create(
      String name,
      String normalizedName,
      String category,
      String locality,
      String normalizedLocality,
      String province,
      String country,
      String website,
      String websiteDomain) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Institution name is required");
    }
    if (normalizedName == null || normalizedName.isBlank()) {
      throw new IllegalArgumentException("Normalized institution name is required");
    }
    return new Institution(
        name.trim(),
        normalizedName,
        category,
        locality,
        normalizedLocality,
        province,
        country,
        website,
        websiteDomain);
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

  public String getNormalizedLocality() {
    return normalizedLocality;
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

  public String getWebsiteDomain() {
    return websiteDomain;
  }
}
