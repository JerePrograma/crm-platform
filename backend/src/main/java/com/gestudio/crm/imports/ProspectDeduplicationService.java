package com.gestudio.crm.imports;

import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.contact.ContactChannelRepository;
import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.imports.DuplicateReview.MatchType;
import com.gestudio.crm.imports.ProspectImportFileParser.ProspectCandidate;
import com.gestudio.crm.institution.Institution;
import com.gestudio.crm.institution.InstitutionRepository;
import com.gestudio.crm.prospect.Prospect;
import com.gestudio.crm.prospect.ProspectRepository;
import com.gestudio.crm.security.CurrentActor;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProspectDeduplicationService {

  private static final double REVIEW_THRESHOLD = 0.80d;

  private final ProspectRepository prospectRepository;
  private final InstitutionRepository institutionRepository;
  private final ContactChannelRepository contactChannelRepository;
  private final NormalizationService normalizationService;
  private final NameSimilarityService nameSimilarityService;
  private final CurrentActor currentActor;

  public ProspectDeduplicationService(
      ProspectRepository prospectRepository,
      InstitutionRepository institutionRepository,
      ContactChannelRepository contactChannelRepository,
      NormalizationService normalizationService,
      NameSimilarityService nameSimilarityService,
      CurrentActor currentActor) {
    this.prospectRepository = prospectRepository;
    this.institutionRepository = institutionRepository;
    this.contactChannelRepository = contactChannelRepository;
    this.normalizationService = normalizationService;
    this.nameSimilarityService = nameSimilarityService;
    this.currentActor = currentActor;
  }

  @Transactional(readOnly = true)
  public DeduplicationOutcome evaluate(ProspectCandidate candidate) {
    NormalizedCandidate normalized = normalize(candidate);

    Optional<Prospect> byExternalId =
        normalized.externalId() == null
            ? Optional.empty()
            : prospectRepository.findByOrganizationIdAndExternalSourceId(
                currentActor.organizationId(), normalized.externalId());
    if (byExternalId.isPresent()) {
      return DeduplicationOutcome.exact(
          MatchType.EXTERNAL_SOURCE_ID, byExternalId.get(), normalized);
    }

    Optional<Prospect> byEmail =
        existingProspectForChannel(ContactChannelType.EMAIL, normalized.email());
    if (byEmail.isPresent()) {
      return DeduplicationOutcome.exact(MatchType.EMAIL, byEmail.get(), normalized);
    }

    Optional<Prospect> byPhone =
        existingProspectForChannel(ContactChannelType.PHONE, normalized.phone())
            .or(() -> existingProspectForChannel(ContactChannelType.WHATSAPP, normalized.phone()));
    if (byPhone.isPresent()) {
      return DeduplicationOutcome.exact(MatchType.PHONE, byPhone.get(), normalized);
    }

    Optional<Prospect> byDomain = existingProspectForDomain(normalized.websiteDomain());
    if (byDomain.isPresent()) {
      return DeduplicationOutcome.exact(MatchType.DOMAIN, byDomain.get(), normalized);
    }

    Optional<Institution> exactInstitution =
        institutionRepository.findByOrganizationIdAndNormalizedNameAndNormalizedLocality(
            currentActor.organizationId(), normalized.institutionName(), normalized.locality());
    if (exactInstitution.isPresent()) {
      Optional<Prospect> exactProspect =
          prospectRepository.findFirstByOrganizationIdAndInstitutionId(
              currentActor.organizationId(), exactInstitution.get().getId());
      if (exactProspect.isPresent()) {
        return DeduplicationOutcome.exact(MatchType.NAME_LOCATION, exactProspect.get(), normalized);
      }
    }

    if (normalized.locality() == null) {
      return DeduplicationOutcome.newRecord(normalized);
    }

    Optional<SimilarityCandidate> ambiguous =
        institutionRepository
            .findAllByOrganizationIdAndNormalizedLocality(
                currentActor.organizationId(), normalized.locality())
            .stream()
            .map(
                institution ->
                    new SimilarityCandidate(
                        institution,
                        nameSimilarityService.similarity(
                            normalized.institutionName(), institution.getNormalizedName())))
            .filter(candidateMatch -> candidateMatch.similarity() >= REVIEW_THRESHOLD)
            .filter(candidateMatch -> candidateMatch.similarity() < 1d)
            .max(Comparator.comparingDouble(SimilarityCandidate::similarity));

    if (ambiguous.isPresent()) {
      Optional<Prospect> prospect =
          prospectRepository.findFirstByOrganizationIdAndInstitutionId(
              currentActor.organizationId(), ambiguous.get().institution().getId());
      return DeduplicationOutcome.review(
          prospect.orElse(null), ambiguous.get().similarity(), normalized);
    }

    return DeduplicationOutcome.newRecord(normalized);
  }

  private Optional<Prospect> existingProspectForChannel(
      ContactChannelType type, String normalizedValue) {
    if (normalizedValue == null) {
      return Optional.empty();
    }
    return contactChannelRepository
        .findByOrganizationIdAndTypeAndNormalizedValue(
            currentActor.organizationId(), type, normalizedValue)
        .flatMap(
            channel ->
                prospectRepository.findFirstByOrganizationIdAndInstitutionId(
                    currentActor.organizationId(), channel.getContact().getInstitution().getId()));
  }

  private Optional<Prospect> existingProspectForDomain(String websiteDomain) {
    if (websiteDomain == null) {
      return Optional.empty();
    }
    return institutionRepository
        .findFirstByOrganizationIdAndWebsiteDomain(currentActor.organizationId(), websiteDomain)
        .flatMap(
            institution ->
                prospectRepository.findFirstByOrganizationIdAndInstitutionId(
                    currentActor.organizationId(), institution.getId()));
  }

  private NormalizedCandidate normalize(ProspectCandidate candidate) {
    return new NormalizedCandidate(
        normalizationService.trimToNull(candidate.externalId()),
        normalizationService.normalizeName(candidate.institutionName()),
        normalizationService.normalizeText(candidate.locality()),
        normalizationService.normalizeEmail(candidate.email()),
        normalizationService.normalizePhone(candidate.phoneOrWhatsapp()),
        normalizationService.normalizeDomain(candidate.website()));
  }

  private record SimilarityCandidate(Institution institution, double similarity) {}

  public record NormalizedCandidate(
      String externalId,
      String institutionName,
      String locality,
      String email,
      String phone,
      String websiteDomain) {}

  public record DeduplicationOutcome(
      Kind kind,
      MatchType matchType,
      Prospect existingProspect,
      double confidence,
      NormalizedCandidate normalized) {

    static DeduplicationOutcome exact(
        MatchType matchType, Prospect existingProspect, NormalizedCandidate normalized) {
      return new DeduplicationOutcome(
          Kind.EXACT_DUPLICATE, matchType, existingProspect, 1d, normalized);
    }

    static DeduplicationOutcome review(
        Prospect existingProspect, double confidence, NormalizedCandidate normalized) {
      return new DeduplicationOutcome(
          Kind.REVIEW_REQUIRED,
          MatchType.NOMINAL_SIMILARITY,
          existingProspect,
          confidence,
          normalized);
    }

    static DeduplicationOutcome newRecord(NormalizedCandidate normalized) {
      return new DeduplicationOutcome(Kind.NEW, null, null, 0d, normalized);
    }
  }

  public enum Kind {
    NEW,
    EXACT_DUPLICATE,
    REVIEW_REQUIRED
  }
}
