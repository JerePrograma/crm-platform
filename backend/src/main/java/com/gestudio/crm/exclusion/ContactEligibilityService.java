package com.gestudio.crm.exclusion;

import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.security.CurrentActor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactEligibilityService {

  private final ExclusionRepository exclusionRepository;
  private final CurrentActor currentActor;

  public ContactEligibilityService(
      ExclusionRepository exclusionRepository, CurrentActor currentActor) {
    this.exclusionRepository = exclusionRepository;
    this.currentActor = currentActor;
  }

  @Transactional(readOnly = true)
  public EligibilityDecision evaluate(Collection<ChannelCandidate> candidates) {
    List<ExcludedChannel> exclusions = new ArrayList<>();
    if (candidates != null) {
      for (ChannelCandidate candidate : candidates) {
        if (candidate == null
            || candidate.type() == null
            || candidate.normalizedValue() == null
            || candidate.normalizedValue().isBlank()) {
          continue;
        }
        findExclusion(candidate)
            .ifPresent(
                exclusion ->
                    exclusions.add(
                        new ExcludedChannel(
                            candidate.type(), candidate.normalizedValue(), exclusion.getReason())));
      }
    }
    return new EligibilityDecision(exclusions.isEmpty(), List.copyOf(exclusions));
  }

  private Optional<Exclusion> findExclusion(ChannelCandidate candidate) {
    Optional<Exclusion> exact =
        exclusionRepository.findByOrganizationIdAndChannelTypeAndNormalizedValue(
            currentActor.organizationId(), candidate.type(), candidate.normalizedValue());
    if (exact.isPresent()) {
      return exact;
    }
    if (candidate.type() == ContactChannelType.PHONE) {
      return exclusionRepository.findByOrganizationIdAndChannelTypeAndNormalizedValue(
          currentActor.organizationId(), ContactChannelType.WHATSAPP, candidate.normalizedValue());
    }
    if (candidate.type() == ContactChannelType.WHATSAPP) {
      return exclusionRepository.findByOrganizationIdAndChannelTypeAndNormalizedValue(
          currentActor.organizationId(), ContactChannelType.PHONE, candidate.normalizedValue());
    }
    return Optional.empty();
  }

  public record ChannelCandidate(ContactChannelType type, String normalizedValue) {}

  public record ExcludedChannel(
      ContactChannelType type, String normalizedValue, ExclusionReason reason) {}

  public record EligibilityDecision(boolean eligible, List<ExcludedChannel> exclusions) {}
}
