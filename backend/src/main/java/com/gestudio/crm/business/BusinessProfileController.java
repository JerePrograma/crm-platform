package com.gestudio.crm.business;

import com.gestudio.crm.business.BusinessProfileService.ProfileView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BusinessProfileController {

  private final BusinessProfileService service;

  public BusinessProfileController(BusinessProfileService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/business-profile")
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  public ProfileView get() {
    return service.get();
  }
}
