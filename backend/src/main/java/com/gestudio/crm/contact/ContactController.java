package com.gestudio.crm.contact;

import com.gestudio.crm.contact.ContactOperationsService.ChannelCommand;
import com.gestudio.crm.contact.ContactOperationsService.ContactView;
import com.gestudio.crm.contact.ContactOperationsService.CreateContactCommand;
import com.gestudio.crm.contact.ContactOperationsService.UpdateChannelCommand;
import com.gestudio.crm.contact.ContactOperationsService.UpdateContactCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ContactController {

  private final ContactOperationsService contactOperationsService;

  public ContactController(ContactOperationsService contactOperationsService) {
    this.contactOperationsService = contactOperationsService;
  }

  @GetMapping("/prospects/{prospectId}/contacts")
  @PreAuthorize("hasAuthority('PROSPECT_READ')")
  public List<ContactView> list(@PathVariable UUID prospectId) {
    return contactOperationsService.listForProspect(prospectId);
  }

  @PostMapping("/prospects/{prospectId}/contacts")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public ContactView create(
      @PathVariable UUID prospectId, @Valid @RequestBody CreateContactRequest request) {
    return contactOperationsService.create(prospectId, request.command());
  }

  @PutMapping("/contacts/{contactId}")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public ContactView update(
      @PathVariable UUID contactId, @Valid @RequestBody UpdateContactRequest request) {
    return contactOperationsService.update(contactId, request.command());
  }

  @DeleteMapping("/contacts/{contactId}")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public void delete(@PathVariable UUID contactId, @RequestParam @PositiveOrZero long version) {
    contactOperationsService.delete(contactId, version);
  }

  @PostMapping("/contacts/{contactId}/primary")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public ContactView makePrimary(
      @PathVariable UUID contactId, @Valid @RequestBody VersionRequest request) {
    return contactOperationsService.makePrimary(contactId, request.version());
  }

  @PostMapping("/contacts/{contactId}/channels")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public ContactView addChannel(
      @PathVariable UUID contactId, @Valid @RequestBody ChannelRequest request) {
    return contactOperationsService.addChannel(contactId, request.command());
  }

  @PutMapping("/contact-channels/{channelId}")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public ContactView updateChannel(
      @PathVariable UUID channelId, @Valid @RequestBody UpdateChannelRequest request) {
    return contactOperationsService.updateChannel(channelId, request.command());
  }

  @DeleteMapping("/contact-channels/{channelId}")
  @PreAuthorize("hasAuthority('PROSPECT_WRITE')")
  public void deleteChannel(
      @PathVariable UUID channelId, @RequestParam @PositiveOrZero long version) {
    contactOperationsService.deleteChannel(channelId, version);
  }

  public record CreateContactRequest(
      String firstName,
      String lastName,
      String role,
      boolean primary,
      boolean verified,
      ContactChannelType preferredChannel,
      String consent,
      String source,
      Instant lastValidatedAt,
      List<@Valid ChannelRequest> channels) {
    CreateContactCommand command() {
      return new CreateContactCommand(
          firstName,
          lastName,
          role,
          primary,
          verified,
          preferredChannel,
          consent,
          source,
          lastValidatedAt,
          channels == null ? List.of() : channels.stream().map(ChannelRequest::command).toList());
    }
  }

  public record UpdateContactRequest(
      @PositiveOrZero long version,
      String firstName,
      String lastName,
      String role,
      boolean primary,
      boolean verified,
      ContactChannelType preferredChannel,
      String consent,
      String source,
      Instant lastValidatedAt) {
    UpdateContactCommand command() {
      return new UpdateContactCommand(
          version,
          firstName,
          lastName,
          role,
          primary,
          verified,
          preferredChannel,
          consent,
          source,
          lastValidatedAt);
    }
  }

  public record ChannelRequest(
      @NotNull ContactChannelType type,
      @NotBlank String value,
      boolean primary,
      Boolean valid,
      boolean verified,
      String consent,
      boolean preferred,
      Instant lastValidatedAt) {
    ChannelCommand command() {
      return new ChannelCommand(
          type,
          value,
          primary,
          valid == null || valid,
          verified,
          consent,
          preferred,
          lastValidatedAt);
    }
  }

  public record UpdateChannelRequest(
      @PositiveOrZero long version,
      @NotNull ContactChannelType type,
      @NotBlank String value,
      boolean primary,
      Boolean valid,
      boolean verified,
      String consent,
      boolean preferred,
      Instant lastValidatedAt) {
    UpdateChannelCommand command() {
      return new UpdateChannelCommand(
          version,
          type,
          value,
          primary,
          valid == null || valid,
          verified,
          consent,
          preferred,
          lastValidatedAt);
    }
  }

  public record VersionRequest(@PositiveOrZero long version) {}
}
