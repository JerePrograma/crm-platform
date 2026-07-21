package com.gestudio.crm.messaging;

import com.gestudio.crm.messaging.MessageDispatcher.CreateMessageCommand;
import com.gestudio.crm.messaging.MessageDispatcher.ManualLink;
import com.gestudio.crm.messaging.MessageDispatcher.MessageView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

  private final MessageDispatcherService dispatcher;

  public MessageController(MessageDispatcherService dispatcher) {
    this.dispatcher = dispatcher;
  }

  @GetMapping("/safety")
  @PreAuthorize("hasAuthority('CAMPAIGN_READ')")
  public MessageDispatcherService.SafetyView safety() {
    return dispatcher.safety();
  }

  @PostMapping("/drafts")
  @PreAuthorize("hasAuthority('MESSAGE_DRAFT')")
  public ResponseEntity<MessageView> draft(
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
      @Valid @RequestBody MessageRequest request) {
    MessageView message = dispatcher.createDraft(request.command(idempotencyKey));
    return ResponseEntity.created(URI.create("/api/v1/messages/" + message.id())).body(message);
  }

  @PostMapping("/simulations")
  @PreAuthorize("hasAuthority('MESSAGE_SIMULATE')")
  public ResponseEntity<MessageView> simulate(
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 200) String idempotencyKey,
      @Valid @RequestBody MessageRequest request) {
    MessageView message = dispatcher.simulate(request.command(idempotencyKey));
    return ResponseEntity.created(URI.create("/api/v1/messages/" + message.id())).body(message);
  }

  @PostMapping("/manual-link")
  @PreAuthorize("hasAuthority('MESSAGE_DRAFT')")
  public ManualLink manualLink(@Valid @RequestBody MessageRequest request) {
    return dispatcher.manualLink(request.command("manual-preview-" + UUID.randomUUID()));
  }

  public record MessageRequest(
      @NotNull UUID prospectId,
      @NotNull UUID contactId,
      @NotBlank @Size(max = 20) String channel,
      @Size(max = 300) String subject,
      @NotBlank @Size(max = 100_000) String textBody,
      @Size(max = 100_000) String htmlBody) {
    CreateMessageCommand command(String idempotencyKey) {
      return new CreateMessageCommand(
          prospectId, contactId, channel, subject, textBody, htmlBody, idempotencyKey);
    }
  }
}
