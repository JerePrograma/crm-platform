package com.gestudio.crm.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.bootstrap")
public record SecurityBootstrapProperties(String username, String password) {

  public boolean configured() {
    return username != null
        && !username.isBlank()
        && password != null
        && !password.isBlank();
  }
}
