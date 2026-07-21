package com.gestudio.crm.security;

import com.gestudio.crm.common.TenantIds;
import com.gestudio.crm.identity.CrmPrincipal;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentActor {

  public UUID organizationId() {
    CrmPrincipal principal = principalOrNull();
    return principal == null ? TenantIds.BOOTSTRAP_ORGANIZATION_ID : principal.organizationId();
  }

  public UUID userIdOrNull() {
    CrmPrincipal principal = principalOrNull();
    return principal == null ? null : principal.userId();
  }

  public CrmPrincipal requiredPrincipal() {
    CrmPrincipal principal = principalOrNull();
    if (principal == null) {
      throw new IllegalStateException("An authenticated CRM user is required");
    }
    return principal;
  }

  private CrmPrincipal principalOrNull() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof CrmPrincipal principal)) {
      return null;
    }
    return principal;
  }
}
