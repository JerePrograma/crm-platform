package com.gestudio.crm.gmail;

import com.gestudio.crm.gmail.GmailSenderAccountService.ConfigurationView;
import com.gestudio.crm.gmail.GmailSenderAccountService.OAuthStartView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sender-accounts")
public class GmailSenderAccountController {

  private final GmailSenderAccountService service;

  public GmailSenderAccountController(GmailSenderAccountService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
  public List<SenderAccountView> list() {
    return service.list();
  }

  @GetMapping("/configuration")
  @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
  public ConfigurationView configuration() {
    return service.configuration();
  }

  @PostMapping("/gmail/oauth/start")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public OAuthStartView start(HttpServletRequest request) {
    return service.start(sessionId(request));
  }

  @GetMapping("/gmail/oauth/callback")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> callback(
      @RequestParam(name = "state", required = false) String state,
      @RequestParam(name = "code", required = false) String code,
      @RequestParam(name = "error", required = false) String error,
      HttpServletRequest request) {
    URI location = service.callback(state, code, error, sessionId(request));
    return ResponseEntity.status(HttpStatus.SEE_OTHER).location(location).build();
  }

  @PostMapping("/{id}/verify")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public SenderAccountView verify(@PathVariable UUID id) {
    return service.verify(id);
  }

  @PostMapping("/{id}/default")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public SenderAccountView setDefault(@PathVariable UUID id) {
    return service.setDefault(id);
  }

  @PostMapping("/{id}/reconnect")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public OAuthStartView reconnect(@PathVariable UUID id, HttpServletRequest request) {
    return service.reconnect(id, sessionId(request));
  }

  @PostMapping("/{id}/revoke")
  @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
  public SenderAccountView revoke(@PathVariable UUID id) {
    return service.revoke(id);
  }

  private String sessionId(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      throw new GmailProblemException(
          HttpStatus.UNAUTHORIZED, "GMAIL_SESSION_REQUIRED", "Authenticated session is required");
    }
    return session.getId();
  }
}
