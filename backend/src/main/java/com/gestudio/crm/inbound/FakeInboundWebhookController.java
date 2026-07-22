package com.gestudio.crm.inbound;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/fake-inbound")
public class FakeInboundWebhookController {

  private final FakeInboundWebhookService webhookService;

  public FakeInboundWebhookController(FakeInboundWebhookService webhookService) {
    this.webhookService = webhookService;
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FakeInboundWebhookService.ReceiptAccepted> receive(
      @RequestHeader("X-Organization-Id") UUID organizationId,
      @RequestHeader("X-Fake-Timestamp") long timestamp,
      @RequestHeader("X-Fake-Nonce") String nonce,
      @RequestHeader("X-Fake-Signature") String signature,
      @RequestBody byte[] payload,
      HttpServletRequest request) {
    FakeInboundWebhookService.ReceiptAccepted accepted =
        webhookService.receive(
            organizationId, timestamp, nonce, signature, payload, request.getRemoteAddr());
    return ResponseEntity.status(accepted.duplicate() ? HttpStatus.OK : HttpStatus.ACCEPTED)
        .body(accepted);
  }
}
