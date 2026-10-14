package com.gestudio.crm.business;

import com.gestudio.crm.common.TenantIds;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crm.business")
public record BusinessProfileProperties(
    String profile,
    String marketingUsername,
    String marketingPassword,
    String marketingDisplayName) {

  public static final UUID SCALARIS_ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000020");

  public String code() {
    String code =
        profile == null || profile.isBlank() ? "LEGACY" : profile.trim().toUpperCase(Locale.ROOT);
    if (!code.equals("LEGACY") && !code.equals("SCALARIS")) {
      throw new IllegalArgumentException("Unsupported CRM business profile: " + code);
    }
    return code;
  }

  public boolean scalaris() {
    return code().equals("SCALARIS");
  }

  public UUID organizationId() {
    return scalaris() ? SCALARIS_ORGANIZATION_ID : TenantIds.BOOTSTRAP_ORGANIZATION_ID;
  }

  public boolean marketingConfigured() {
    boolean hasUsername = marketingUsername != null && !marketingUsername.isBlank();
    boolean hasPassword = marketingPassword != null && !marketingPassword.isBlank();
    if (hasUsername != hasPassword) {
      throw new IllegalArgumentException("Both CRM Marketing credentials are required");
    }
    return hasUsername;
  }

  public String effectiveMarketingDisplayName() {
    return marketingDisplayName == null || marketingDisplayName.isBlank()
        ? "Marketing Scalaris"
        : marketingDisplayName.trim();
  }
}
