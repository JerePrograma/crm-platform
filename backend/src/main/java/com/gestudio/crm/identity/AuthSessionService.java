package com.gestudio.crm.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {

  private static final int SESSION_TIMEOUT_SECONDS = 8 * 60 * 60;

  private final AuthenticationManager authenticationManager;
  private final IdentityService identityService;
  private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  public AuthSessionService(
      AuthenticationManager authenticationManager,
      IdentityService identityService,
      SessionAuthenticationStrategy sessionAuthenticationStrategy) {
    this.authenticationManager = authenticationManager;
    this.identityService = identityService;
    this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
  }

  public CrmPrincipal login(
      String username, String password, HttpServletRequest request, HttpServletResponse response) {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(username, password));
      sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      securityContextRepository.saveContext(context, request, response);
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
      }
      CrmPrincipal principal = (CrmPrincipal) authentication.getPrincipal();
      identityService.recordSuccessfulLogin(principal);
      return principal;
    } catch (AuthenticationException exception) {
      identityService.recordFailedLogin(username);
      throw exception;
    }
  }

  public void logout(CrmPrincipal principal, HttpServletRequest request) {
    identityService.recordLogout(principal);
    invalidate(request);
  }

  public void invalidate(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
  }
}
