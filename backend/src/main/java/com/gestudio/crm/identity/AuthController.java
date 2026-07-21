package com.gestudio.crm.identity;

import com.gestudio.crm.security.CurrentActor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthSessionService authSessionService;
  private final IdentityService identityService;
  private final CurrentActor currentActor;

  public AuthController(
      AuthSessionService authSessionService,
      IdentityService identityService,
      CurrentActor currentActor) {
    this.authSessionService = authSessionService;
    this.identityService = identityService;
    this.currentActor = currentActor;
  }

  @GetMapping("/csrf")
  public CsrfView csrf(CsrfToken token) {
    return new CsrfView(token.getToken(), token.getHeaderName(), token.getParameterName());
  }

  @PostMapping("/login")
  public SessionView login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {
    return view(
        authSessionService.login(
            request.username(), request.password(), servletRequest, servletResponse));
  }

  @GetMapping("/me")
  public SessionView me() {
    return view(currentActor.requiredPrincipal());
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest request) {
    authSessionService.logout(currentActor.requiredPrincipal(), request);
  }

  @PostMapping("/password")
  public void changePassword(
      @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest servletRequest) {
    identityService.changePassword(
        currentActor.requiredPrincipal(), request.currentPassword(), request.newPassword());
    authSessionService.invalidate(servletRequest);
  }

  private SessionView view(CrmPrincipal principal) {
    return new SessionView(
        principal.userId(),
        principal.organizationId(),
        principal.username(),
        principal.displayName(),
        principal.role(),
        principal.permissions());
  }

  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  public record ChangePasswordRequest(
      @NotBlank String currentPassword, @NotBlank String newPassword) {}

  public record CsrfView(String token, String headerName, String parameterName) {}

  public record SessionView(
      UUID userId,
      UUID organizationId,
      String username,
      String displayName,
      String role,
      Set<String> permissions) {}
}
