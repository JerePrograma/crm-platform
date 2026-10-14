package com.gestudio.crm.identity;

import com.gestudio.crm.business.BusinessProfileProperties;
import com.gestudio.crm.business.BusinessProfileService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class IdentityBootstrap implements ApplicationRunner {

  private final IdentityService identityService;
  private final BusinessProfileProperties businessProperties;
  private final BusinessProfileService businessProfileService;

  public IdentityBootstrap(
      IdentityService identityService,
      BusinessProfileProperties businessProperties,
      BusinessProfileService businessProfileService) {
    this.identityService = identityService;
    this.businessProperties = businessProperties;
    this.businessProfileService = businessProfileService;
  }

  @Override
  public void run(ApplicationArguments args) {
    businessProfileService.initializeIfRequired();
    identityService.bootstrapIfRequired(businessProperties.organizationId());
    businessProfileService.bootstrapMarketingIfRequired();
  }
}
