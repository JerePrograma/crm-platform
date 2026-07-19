package com.gestudio.crm.exclusion;

import com.gestudio.crm.contact.ContactChannelType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactEligibilityService {

  private final ExclusionRepository exclusionRepository;

  public ContactEligibilityService(ExclusionRepository exclusionRepository) {
    this.exclusionRepository = exclusionRepository;
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
        exclusionRepository
            .findByChannelTypeAndNormalizedValue(
                candidate.type(), candidate.normalizedValue())
            .ifPresent(
                exclusion ->
                    exclusions.add(
                        new ExcludedChannel(
                            candidate.type(), candidate.normalizedValue(), exclusion.getReason())));
      }
    }
    return new EligibilityDecision(exclusions.isEmpty(), List.copyOf(exclusions));
  }

  public record ChannelCandidate(ContactChannelType type, String normalizedValue) {}

  public record ExcludedChannel(
      ContactChannelType type, String normalizedValue, ExclusionReason reason) {}

  public record EligibilityDecision(boolean eligible, List<ExcludedChannel> exclusions) {}
}
