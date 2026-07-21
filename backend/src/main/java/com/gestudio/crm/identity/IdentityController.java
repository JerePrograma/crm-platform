package com.gestudio.crm.identity;

import com.gestudio.crm.identity.IdentityService.UserView;
import com.gestudio.crm.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class IdentityController {

  private final IdentityService identityService;
  private final CurrentActor currentActor;

  public IdentityController(IdentityService identityService, CurrentActor currentActor) {
    this.identityService = identityService;
    this.currentActor = currentActor;
  }

  @GetMapping
  public List<UserView> list() {
    return identityService.listUsers(currentActor.organizationId());
  }

  @PostMapping
  public ResponseEntity<UserView> create(@Valid @RequestBody CreateUserRequest request) {
    UserView user =
        identityService.createUser(
            currentActor.organizationId(),
            request.username(),
            request.displayName(),
            request.password(),
            request.role(),
            currentActor.userIdOrNull());
    return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
  }

  @PatchMapping("/{id}/active")
  public void setActive(@PathVariable UUID id, @Valid @RequestBody ActiveRequest request) {
    if (id.equals(currentActor.requiredPrincipal().userId()) && !request.active()) {
      throw new IllegalArgumentException("The current user cannot deactivate itself");
    }
    identityService.setActive(
        currentActor.organizationId(), id, request.active(), currentActor.userIdOrNull());
  }

  @PostMapping("/{id}/password")
  public void resetPassword(
      @PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
    identityService.resetPassword(
        currentActor.organizationId(), id, request.password(), currentActor.userIdOrNull());
  }

  public record CreateUserRequest(
      @NotBlank String username,
      @NotBlank String displayName,
      @NotBlank String password,
      @NotBlank String role) {}

  public record ActiveRequest(@NotNull Boolean active) {}

  public record ResetPasswordRequest(@NotBlank String password) {}
}
