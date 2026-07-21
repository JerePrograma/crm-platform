package com.gestudio.crm.prospect;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.DuplicateResourceException;
import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.contact.Contact;
import com.gestudio.crm.contact.ContactChannel;
import com.gestudio.crm.contact.ContactChannelRepository;
import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.contact.ContactRepository;
import com.gestudio.crm.exclusion.ContactEligibilityService;
import com.gestudio.crm.exclusion.ContactEligibilityService.ChannelCandidate;
import com.gestudio.crm.institution.Institution;
import com.gestudio.crm.institution.InstitutionRepository;
import com.gestudio.crm.security.CurrentActor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProspectApplicationService {

  private final InstitutionRepository institutionRepository;
  private final ContactRepository contactRepository;
  private final ContactChannelRepository contactChannelRepository;
  private final ProspectRepository prospectRepository;
  private final ContactEligibilityService contactEligibilityService;
  private final NormalizationService normalizationService;
  private final AuditEventWriter auditEventWriter;
  private final CurrentActor currentActor;

  public ProspectApplicationService(
      InstitutionRepository institutionRepository,
      ContactRepository contactRepository,
      ContactChannelRepository contactChannelRepository,
      ProspectRepository prospectRepository,
      ContactEligibilityService contactEligibilityService,
      NormalizationService normalizationService,
      AuditEventWriter auditEventWriter,
      CurrentActor currentActor) {
    this.institutionRepository = institutionRepository;
    this.contactRepository = contactRepository;
    this.contactChannelRepository = contactChannelRepository;
    this.prospectRepository = prospectRepository;
    this.contactEligibilityService = contactEligibilityService;
    this.normalizationService = normalizationService;
    this.auditEventWriter = auditEventWriter;
    this.currentActor = currentActor;
  }

  @Transactional
  public ProspectView create(CreateProspectCommand command) {
    Objects.requireNonNull(command, "Create prospect command is required");

    String externalSourceId = normalizationService.trimToNull(command.externalSourceId());
    if (externalSourceId != null
        && prospectRepository
            .findByOrganizationIdAndExternalSourceId(
                currentActor.organizationId(), externalSourceId)
            .isPresent()) {
      throw new DuplicateResourceException(
          "A prospect already exists for external source id " + externalSourceId);
    }

    String normalizedName = normalizationService.normalizeName(command.institutionName());
    String normalizedLocality = normalizationService.normalizeText(command.locality());
    String websiteDomain = normalizationService.normalizeDomain(command.website());

    Institution institution =
        resolveInstitution(command, normalizedName, normalizedLocality, websiteDomain);

    List<PreparedChannel> preparedChannels = prepareContactChannels(command);
    rejectExistingChannels(preparedChannels);

    List<ChannelCandidate> eligibilityCandidates =
        preparedChannels.stream()
            .map(channel -> new ChannelCandidate(channel.type(), channel.normalizedValue()))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    if (websiteDomain != null) {
      eligibilityCandidates.add(new ChannelCandidate(ContactChannelType.WEBSITE, websiteDomain));
    }

    boolean eligible = contactEligibilityService.evaluate(eligibilityCandidates).eligible();

    if (hasContactData(command, preparedChannels)) {
      Contact contact = Contact.create(institution, command.contactName(), command.contactRole());
      contact.assignOrganization(currentActor.organizationId());
      contactRepository.save(contact);
      boolean first = true;
      for (PreparedChannel channel : preparedChannels) {
        ContactChannel contactChannel =
            ContactChannel.create(
                contact, channel.type(), channel.originalValue(), channel.normalizedValue(), first);
        contactChannel.assignOrganization(currentActor.organizationId());
        contactChannelRepository.save(contactChannel);
        first = false;
      }
    }

    Prospect prospect =
        Prospect.create(
            institution,
            externalSourceId,
            command.priority(),
            command.score(),
            command.estimatedStudents(),
            command.currentTools(),
            command.administrativePain(),
            command.source(),
            command.evidence(),
            command.verifiedAt(),
            command.owner(),
            eligible);
    prospect.assignOrganization(currentActor.organizationId());
    prospectRepository.save(prospect);

    Map<String, Object> auditPayload = new LinkedHashMap<>();
    auditPayload.put("institutionId", institution.getId());
    auditPayload.put("status", prospect.getStatus().name());
    auditPayload.put("contactEligible", prospect.isContactEligible());
    auditPayload.put(
        "channelTypes", preparedChannels.stream().map(channel -> channel.type().name()).toList());
    if (command.source() != null && !command.source().isBlank()) {
      auditPayload.put("source", command.source().trim());
    }
    auditEventWriter.record("PROSPECT_CREATED", "Prospect", prospect.getId(), auditPayload);

    return toView(prospect);
  }

  @Transactional(readOnly = true)
  public ProspectView get(UUID id) {
    Prospect prospect =
        prospectRepository
            .findByIdAndOrganizationId(id, currentActor.organizationId())
            .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + id));
    return toView(prospect);
  }

  @Transactional(readOnly = true)
  public Page<ProspectView> list(ProspectStatus status, Pageable pageable) {
    Page<Prospect> prospects =
        status == null
            ? prospectRepository.findAllByOrganizationId(currentActor.organizationId(), pageable)
            : prospectRepository.findAllByOrganizationIdAndStatus(
                currentActor.organizationId(), status, pageable);
    return prospects.map(this::toView);
  }

  private Institution resolveInstitution(
      CreateProspectCommand command,
      String normalizedName,
      String normalizedLocality,
      String websiteDomain) {
    Optional<Institution> byNameAndLocation =
        institutionRepository.findByOrganizationIdAndNormalizedNameAndNormalizedLocality(
            currentActor.organizationId(), normalizedName, normalizedLocality);
    Optional<Institution> byDomain =
        websiteDomain == null
            ? Optional.empty()
            : institutionRepository.findFirstByOrganizationIdAndWebsiteDomain(
                currentActor.organizationId(), websiteDomain);

    if (byNameAndLocation.isPresent()
        && byDomain.isPresent()
        && !byNameAndLocation.get().getId().equals(byDomain.get().getId())) {
      throw new DuplicateResourceException(
          "Institution name/location and website domain match different records");
    }

    return byNameAndLocation
        .or(() -> byDomain)
        .orElseGet(
            () -> {
              Institution institution =
                  Institution.create(
                      command.institutionName(),
                      normalizedName,
                      normalizationService.trimToNull(command.category()),
                      normalizationService.trimToNull(command.locality()),
                      normalizedLocality,
                      normalizationService.trimToNull(command.province()),
                      normalizationService.trimToNull(command.country()),
                      normalizationService.trimToNull(command.website()),
                      websiteDomain);
              institution.assignOrganization(currentActor.organizationId());
              return institutionRepository.save(institution);
            });
  }

  private List<PreparedChannel> prepareContactChannels(CreateProspectCommand command) {
    List<PreparedChannel> channels = new ArrayList<>();
    addChannel(channels, ContactChannelType.EMAIL, command.email());
    addChannel(channels, ContactChannelType.WHATSAPP, command.whatsapp());
    addChannel(channels, ContactChannelType.PHONE, command.phone());
    return List.copyOf(channels);
  }

  private void addChannel(
      List<PreparedChannel> channels, ContactChannelType type, String originalValue) {
    String normalized = normalizationService.normalizeChannel(type, originalValue);
    if (normalized != null) {
      channels.add(new PreparedChannel(type, originalValue.trim(), normalized));
    }
  }

  private void rejectExistingChannels(List<PreparedChannel> channels) {
    for (PreparedChannel channel : channels) {
      if (contactChannelRepository.existsByOrganizationIdAndTypeAndNormalizedValue(
          currentActor.organizationId(), channel.type(), channel.normalizedValue())) {
        throw new DuplicateResourceException("Contact channel already exists: " + channel.type());
      }
    }
  }

  private boolean hasContactData(
      CreateProspectCommand command, List<PreparedChannel> preparedChannels) {
    return normalizationService.trimToNull(command.contactName()) != null
        || normalizationService.trimToNull(command.contactRole()) != null
        || !preparedChannels.isEmpty();
  }

  private ProspectView toView(Prospect prospect) {
    Institution institution = prospect.getInstitution();
    return new ProspectView(
        prospect.getId(),
        prospect.getVersion(),
        institution.getId(),
        institution.getName(),
        institution.getCategory(),
        institution.getLocality(),
        institution.getProvince(),
        institution.getCountry(),
        institution.getWebsite(),
        prospect.getStatus(),
        prospect.getPriority(),
        prospect.getScore(),
        prospect.getEstimatedStudents(),
        prospect.getSource(),
        prospect.getOwner(),
        prospect.isContactEligible(),
        prospect.getCreatedAt(),
        prospect.getUpdatedAt());
  }

  private record PreparedChannel(
      ContactChannelType type, String originalValue, String normalizedValue) {}

  public record CreateProspectCommand(
      String institutionName,
      String category,
      String locality,
      String province,
      String country,
      String website,
      String contactName,
      String contactRole,
      String email,
      String phone,
      String whatsapp,
      String externalSourceId,
      String source,
      String evidence,
      Integer estimatedStudents,
      Integer priority,
      Integer score,
      String currentTools,
      String administrativePain,
      Instant verifiedAt,
      String owner) {}

  public record ProspectView(
      UUID id,
      long version,
      UUID institutionId,
      String institutionName,
      String category,
      String locality,
      String province,
      String country,
      String website,
      ProspectStatus status,
      Integer priority,
      Integer score,
      Integer estimatedStudents,
      String source,
      String owner,
      boolean contactEligible,
      Instant createdAt,
      Instant updatedAt) {}
}
