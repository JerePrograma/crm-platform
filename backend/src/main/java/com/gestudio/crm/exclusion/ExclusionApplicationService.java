package com.gestudio.crm.exclusion;

import com.gestudio.crm.audit.AuditEventWriter;
import com.gestudio.crm.common.DuplicateResourceException;
import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.common.ResourceNotFoundException;
import com.gestudio.crm.contact.ContactChannelRepository;
import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.institution.InstitutionRepository;
import com.gestudio.crm.prospect.Prospect;
import com.gestudio.crm.prospect.ProspectRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExclusionApplicationService {

  private final ExclusionRepository exclusionRepository;
  private final ContactChannelRepository contactChannelRepository;
  private final InstitutionRepository institutionRepository;
  private final ProspectRepository prospectRepository;
  private final NormalizationService normalizationService;
  private final AuditEventWriter auditEventWriter;

  public ExclusionApplicationService(
      ExclusionRepository exclusionRepository,
      ContactChannelRepository contactChannelRepository,
      InstitutionRepository institutionRepository,
      ProspectRepository prospectRepository,
      NormalizationService normalizationService,
      AuditEventWriter auditEventWriter) {
    this.exclusionRepository = exclusionRepository;
    this.contactChannelRepository = contactChannelRepository;
    this.institutionRepository = institutionRepository;
    this.prospectRepository = prospectRepository;
    this.normalizationService = normalizationService;
    this.auditEventWriter = auditEventWriter;
  }

  @Transactional
  public ExclusionView create(
      ContactChannelType channelType, String value, ExclusionReason reason) {
    String normalizedValue = normalizationService.normalizeChannel(channelType, value);
    if (normalizedValue == null) {
      throw new IllegalArgumentException("A valid exclusion channel value is required");
    }
    if (isAlreadyExcluded(channelType, normalizedValue)) {
      throw new DuplicateResourceException("The channel is already excluded");
    }

    Exclusion exclusion =
        exclusionRepository.save(Exclusion.create(channelType, normalizedValue, reason));
    Optional<Prospect> affectedProspect = existingProspect(channelType, normalizedValue);
    affectedProspect.ifPresent(Prospect::markIneligible);

    auditEventWriter.record(
        "EXCLUSION_CREATED",
        "Exclusion",
        exclusion.getId(),
        Map.of(
            "channelType", channelType.name(),
            "channelFingerprint", fingerprint(normalizedValue),
            "reason", reason.name(),
            "affectedProspectCount", affectedProspect.isPresent() ? 1 : 0));
    return toView(exclusion);
  }

  @Transactional(readOnly = true)
  public ExclusionView get(UUID id) {
    return exclusionRepository
        .findById(id)
        .map(this::toView)
        .orElseThrow(() -> new ResourceNotFoundException("Exclusion not found: " + id));
  }

  @Transactional(readOnly = true)
  public Page<ExclusionView> list(Pageable pageable) {
    return exclusionRepository.findAll(pageable).map(this::toView);
  }

  private boolean isAlreadyExcluded(
      ContactChannelType channelType, String normalizedValue) {
    if (exclusionRepository.existsByChannelTypeAndNormalizedValue(
        channelType, normalizedValue)) {
      return true;
    }
    if (channelType == ContactChannelType.PHONE) {
      return exclusionRepository.existsByChannelTypeAndNormalizedValue(
          ContactChannelType.WHATSAPP, normalizedValue);
    }
    if (channelType == ContactChannelType.WHATSAPP) {
      return exclusionRepository.existsByChannelTypeAndNormalizedValue(
          ContactChannelType.PHONE, normalizedValue);
    }
    return false;
  }

  private Optional<Prospect> existingProspect(
      ContactChannelType channelType, String normalizedValue) {
    if (channelType == ContactChannelType.WEBSITE) {
      return institutionRepository
          .findFirstByWebsiteDomain(normalizedValue)
          .flatMap(institution -> prospectRepository.findFirstByInstitutionId(institution.getId()));
    }
    Optional<Prospect> exact = existingProspectForChannel(channelType, normalizedValue);
    if (exact.isPresent()) {
      return exact;
    }
    if (channelType == ContactChannelType.PHONE) {
      return existingProspectForChannel(ContactChannelType.WHATSAPP, normalizedValue);
    }
    if (channelType == ContactChannelType.WHATSAPP) {
      return existingProspectForChannel(ContactChannelType.PHONE, normalizedValue);
    }
    return Optional.empty();
  }

  private Optional<Prospect> existingProspectForChannel(
      ContactChannelType channelType, String normalizedValue) {
    return contactChannelRepository
        .findByTypeAndNormalizedValue(channelType, normalizedValue)
        .flatMap(
            channel ->
                prospectRepository.findFirstByInstitutionId(
                    channel.getContact().getInstitution().getId()));
  }

  private ExclusionView toView(Exclusion exclusion) {
    return new ExclusionView(
        exclusion.getId(),
        exclusion.getVersion(),
        exclusion.getChannelType(),
        exclusion.getNormalizedValue(),
        exclusion.getReason(),
        exclusion.getCreatedAt(),
        exclusion.getUpdatedAt());
  }

  private String fingerprint(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  public record ExclusionView(
      UUID id,
      long version,
      ContactChannelType channelType,
      String normalizedValue,
      ExclusionReason reason,
      Instant createdAt,
      Instant updatedAt) {}
}
