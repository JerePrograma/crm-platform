package com.gestudio.crm.gmail;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UnsubscribeController {

  private final UnsubscribeService service;
  private final UnsubscribeRateLimiter rateLimiter;
  private final UnsubscribeTokenService tokens;

  public UnsubscribeController(
      UnsubscribeService service,
      UnsubscribeRateLimiter rateLimiter,
      UnsubscribeTokenService tokens) {
    this.service = service;
    this.rateLimiter = rateLimiter;
    this.tokens = tokens;
  }

  @GetMapping(value = "/api/v1/unsubscribe/{opaqueToken}", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> page(@PathVariable String opaqueToken, HttpServletRequest request) {
    UUID tokenId = tokenId(opaqueToken);
    rateLimiter.requireAllowed(key(request, tokenId));
    boolean valid = tokenId != null && service.valid(tokenId, opaqueToken);
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(valid ? confirmationPage(opaqueToken) : genericPage());
  }

  @PostMapping(value = "/api/v1/unsubscribe/{opaqueToken}", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> unsubscribe(
      @PathVariable String opaqueToken, HttpServletRequest request) {
    UUID tokenId = tokenId(opaqueToken);
    rateLimiter.requireAllowed(key(request, tokenId));
    if (tokenId != null) {
      service.unsubscribe(tokenId, opaqueToken);
    }
    return ResponseEntity.ok().header("Cache-Control", "no-store").body(donePage());
  }

  private String confirmationPage(String opaqueToken) {
    return page(
        "Confirmar baja",
        "Podés dejar de recibir futuros correos de esta organización.",
        "<form method=\"post\" action=\"/api/v1/unsubscribe/"
            + html(opaqueToken)
            + "\"><input type=\"hidden\" name=\"List-Unsubscribe\" value=\"One-Click\">"
            + "<button type=\"submit\">Confirmar baja</button></form>");
  }

  private String donePage() {
    return page(
        "Baja registrada",
        "La exclusión ya está activa. No se enviarán futuros seguimientos a este canal.",
        "");
  }

  private String genericPage() {
    return page(
        "Solicitud procesada", "Si el enlace era válido, la exclusión ya se encuentra activa.", "");
  }

  private String page(String title, String message, String action) {
    return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
        + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
        + "<title>"
        + title
        + "</title><style>body{font:16px system-ui;margin:4rem auto;max-width:42rem;padding:1rem}"
        + "button{padding:.75rem 1rem}</style></head><body><main><h1>"
        + title
        + "</h1><p>"
        + message
        + "</p>"
        + action
        + "</main></body></html>";
  }

  private String html(String value) {
    return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
  }

  private UUID tokenId(String opaqueToken) {
    return tokens.tokenId(opaqueToken);
  }

  private String key(HttpServletRequest request, UUID tokenId) {
    return request.getRemoteAddr() + ":" + (tokenId == null ? "invalid" : tokenId);
  }
}
