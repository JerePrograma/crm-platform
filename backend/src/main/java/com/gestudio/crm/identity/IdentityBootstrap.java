package com.gestudio.crm.identity;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class IdentityBootstrap implements ApplicationRunner {

  private final IdentityService identityService;

  public IdentityBootstrap(IdentityService identityService) {
    this.identityService = identityService;
  }

  @Override
  public void run(ApplicationArguments args) {
    identityService.bootstrapIfRequired();
  }
}
