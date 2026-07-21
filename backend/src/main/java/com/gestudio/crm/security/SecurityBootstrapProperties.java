package com.gestudio.crm.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.bootstrap")
public record SecurityBootstrapProperties(
    String username, String password, int minimumPasswordLength) {

  public boolean configured() {
    return username != null && !username.isBlank() && password != null && !password.isBlank();
  }

  public int effectiveMinimumPasswordLength() {
    return minimumPasswordLength <= 0 ? 12 : minimumPasswordLength;
  }
}
