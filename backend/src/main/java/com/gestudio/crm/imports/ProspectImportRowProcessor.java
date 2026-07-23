package com.gestudio.crm.imports;

import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.exclusion.ContactEligibilityService;
import com.gestudio.crm.exclusion.ContactEligibilityService.ChannelCandidate;
import com.gestudio.crm.exclusion.ExclusionApplicationService;
import com.gestudio.crm.exclusion.ExclusionReason;
import com.gestudio.crm.exclusion.ExclusionRepository;
import com.gestudio.crm.imports.ImportJobLifecycleService.RowOutcome;
import com.gestudio.crm.imports.ProspectDeduplicationService.DeduplicationOutcome;
import com.gestudio.crm.imports.ProspectDeduplicationService.Kind;
import com.gestudio.crm.imports.ProspectImportFileParser.ExclusionCandidate;
import com.gestudio.crm.imports.ProspectImportFileParser.ProspectCandidate;
import com.gestudio.crm.prospect.Prospect;
import com.gestudio.crm.prospect.ProspectApplicationService;
import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import com.gestudio.crm.prospect.ProspectApplicationService.ProspectView;
import com.gestudio.crm.prospect.ProspectRepository;
import com.gestudio.crm.security.CurrentActor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProspectImportRowProcessor {

  private final ImportJobRepository importJobRepository;
  private final ImportRowRepository importRowRepository;
  private final DuplicateReviewRepository duplicateReviewRepository;
  private final ExclusionRepository exclusionRepository;
  private final ExclusionApplicationService exclusionApplicationService;
  private final ContactEligibilityService contactEligibilityService;
  private final ProspectRepository prospectRepository;
  private final ProspectDeduplicationService deduplicationService;
  private final ProspectApplicationService prospectApplicationService;
  private final NormalizationService normalizationService;
  private final ObjectMapper objectMapper;
  private final CurrentActor currentActor;

  public ProspectImportRowProcessor(
      ImportJobRepository importJobRepository,
      ImportRowRepository importRowRepository,
      DuplicateReviewRepository duplicateReviewRepository,
      ExclusionRepository exclusionRepository,
      ExclusionApplicationService exclusionApplicationService,
      ContactEligibilityService contactEligibilityService,
      ProspectRepository prospectRepository,
      ProspectDeduplicationService deduplicationService,
      ProspectApplicationService prospectApplicationService,
      NormalizationService normalizationService,
      ObjectMapper objectMapper,
      CurrentActor currentActor) {
    this.importJobRepository = importJobRepository;
    this.importRowRepository = importRowRepository;
    this.duplicateReviewRepository = duplicateReviewRepository;
    this.exclusionRepository = exclusionRepository;
    this.exclusionApplicationService = exclusionApplicationService;
    this.contactEligibilityService = contactEligibilityService;
    this.prospectRepository = prospectRepository;
    this.deduplicationService = deduplicationService;
    this.prospectApplicationService = prospectApplicationService;
    this.normalizationService = normalizationService;
    this.objectMapper = objectMapper;
    this.currentActor = currentActor;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RowOutcome processProspect(UUID jobId, ProspectCandidate candidate, boolean dryRun) {
    return processProspect(jobId, candidate, dryRun, false);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RowOutcome processProspect(
      UUID jobId, ProspectCandidate candidate, boolean dryRun, boolean excludedByImportFile) {
    ImportJob job = getJob(jobId);
    String normalizedEmail = normalizationService.normalizeEmail(candidate.email());
    String normalizedPhone = normalizationService.normalizePhone(candidate.phoneOrWhatsapp());
    ImportRow row =
        ImportRow.create(
            job,
            "Prospectos",
            candidate.rowNumber(),
            rawJson(candidate.rawData()),
            normalizedEmail,
            normalizedPhone);
    row.assignOrganization(currentActor.organizationId());
    importRowRepository.save(row);

    if (normalizationService.trimToNull(candidate.institutionName()) == null) {
      row.reject("Institution name is required");
      return RowOutcome.REJECTED;
    }

    DeduplicationOutcome deduplication = deduplicationService.evaluate(candidate);
    if (deduplication.kind() == Kind.EXACT_DUPLICATE) {
      row.markDuplicate(deduplication.existingProspect());
      return RowOutcome.DUPLICATE;
    }
    if (deduplication.kind() == Kind.REVIEW_REQUIRED) {
      row.requireReview();
      DuplicateReview review =
          DuplicateReview.create(
              row,
              deduplication.existingProspect(),
              deduplication.matchType(),
              BigDecimal.valueOf(deduplication.confidence()).setScale(4, RoundingMode.HALF_UP),
              dryRun
                  ? "Ambiguous nominal match detected during preview; automatic merge is forbidden"
                  : "Ambiguous nominal match; automatic merge is forbidden");
      review.assignOrganization(currentActor.organizationId());
      duplicateReviewRepository.save(review);
      return RowOutcome.REVIEW_REQUIRED;
    }

    if (dryRun) {
      if (!excludedByImportFile && previewEligible(candidate, normalizedEmail, normalizedPhone)) {
        row.accept(null);
        return RowOutcome.ACCEPTED;
      }
      row.exclude(null);
      return RowOutcome.EXCLUDED;
    }

    ProspectView created = prospectApplicationService.create(toCommand(candidate));
    Prospect prospect =
        prospectRepository
            .findByIdAndOrganizationId(created.id(), currentActor.organizationId())
            .orElseThrow(() -> new IllegalStateException("Created prospect could not be reloaded"));
    if (created.contactEligible()) {
      row.accept(prospect);
      return RowOutcome.ACCEPTED;
    }
    row.exclude(prospect);
    return RowOutcome.EXCLUDED;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RowOutcome processExclusion(UUID jobId, ExclusionCandidate candidate, boolean dryRun) {
    ImportJob job = getJob(jobId);
    String normalizedEmail = normalizationService.normalizeEmail(candidate.email());
    ImportRow row =
        ImportRow.create(
            job,
            "Exclusiones",
            candidate.rowNumber(),
            rawJson(candidate.rawData()),
            normalizedEmail,
            null);
    row.assignOrganization(currentActor.organizationId());
    importRowRepository.save(row);
    if (normalizedEmail == null) {
      row.reject("Exclusion email is required");
      return RowOutcome.REJECTED;
    }
    if (exclusionRepository.existsByOrganizationIdAndChannelTypeAndNormalizedValue(
        currentActor.organizationId(), ContactChannelType.EMAIL, normalizedEmail)) {
      row.markDuplicate(null);
      return RowOutcome.DUPLICATE;
    }
    if (!dryRun) {
      exclusionApplicationService.create(
          ContactChannelType.EMAIL, normalizedEmail, mapReason(candidate.reason()));
    }
    row.accept(null);
    return RowOutcome.ACCEPTED;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordRejectedProspect(UUID jobId, ProspectCandidate candidate, String message) {
    ImportJob job = getJob(jobId);
    ImportRow row =
        ImportRow.create(
            job,
            "Prospectos",
            candidate.rowNumber(),
            rawJson(candidate.rawData()),
            safeNormalizeEmail(candidate.email()),
            safeNormalizePhone(candidate.phoneOrWhatsapp()));
    row.assignOrganization(currentActor.organizationId());
    row.reject(message);
    importRowRepository.save(row);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordRejectedExclusion(UUID jobId, ExclusionCandidate candidate, String message) {
    ImportJob job = getJob(jobId);
    ImportRow row =
        ImportRow.create(
            job,
            "Exclusiones",
            candidate.rowNumber(),
            rawJson(candidate.rawData()),
            safeNormalizeEmail(candidate.email()),
            null);
    row.assignOrganization(currentActor.organizationId());
    row.reject(message);
    importRowRepository.save(row);
  }

  private boolean previewEligible(
      ProspectCandidate candidate, String normalizedEmail, String normalizedPhone) {
    List<ChannelCandidate> channels = new ArrayList<>();
    if (normalizedEmail != null) {
      channels.add(new ChannelCandidate(ContactChannelType.EMAIL, normalizedEmail));
    }
    if (normalizedPhone != null) {
      channels.add(new ChannelCandidate(ContactChannelType.WHATSAPP, normalizedPhone));
    }
    String websiteDomain = normalizationService.normalizeDomain(candidate.website());
    if (websiteDomain != null) {
      channels.add(new ChannelCandidate(ContactChannelType.WEBSITE, websiteDomain));
    }
    boolean hasDirectContactChannel = normalizedEmail != null || normalizedPhone != null;
    return hasDirectContactChannel && contactEligibilityService.evaluate(channels).eligible();
  }

  private ImportJob getJob(UUID jobId) {
    return importJobRepository
        .findByIdAndOrganizationId(jobId, currentActor.organizationId())
        .orElseThrow(() -> new IllegalArgumentException("Import job not found: " + jobId));
  }

  private CreateProspectCommand toCommand(ProspectCandidate candidate) {
    return new CreateProspectCommand(
        candidate.institutionName(),
        candidate.category(),
        candidate.locality(),
        candidate.province(),
        "Argentina",
        candidate.website(),
        null,
        "Contacto publicado",
        candidate.email(),
        null,
        candidate.phoneOrWhatsapp(),
        candidate.externalId(),
        candidate.source(),
        candidate.evidence(),
        null,
        candidate.priority(),
        null,
        null,
        candidate.administrativePain(),
        candidate.verifiedAt(),
        null);
  }

  private ExclusionReason mapReason(String value) {
    String normalized = normalizationService.normalizeText(value);
    if (normalized == null) {
      return ExclusionReason.MANUAL;
    }
    if (normalized.contains("gmail")
        || normalized.contains("conversacion")
        || normalized.contains("conversation")
        || normalized.contains("correo enviado")) {
      return ExclusionReason.EXISTING_CONVERSATION;
    }
    if (normalized.contains("cliente")) {
      return ExclusionReason.EXISTING_CUSTOMER;
    }
    if (normalized.contains("rebote")) {
      return ExclusionReason.PERMANENT_BOUNCE;
    }
    if (normalized.contains("baja") || normalized.contains("no recibir")) {
      return ExclusionReason.UNSUBSCRIBE_REQUEST;
    }
    return ExclusionReason.MANUAL;
  }

  private String safeNormalizeEmail(String value) {
    try {
      return normalizationService.normalizeEmail(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private String safeNormalizePhone(String value) {
    try {
      return normalizationService.normalizePhone(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private String rawJson(java.util.Map<String, String> rawData) {
    try {
      return objectMapper.writeValueAsString(rawData);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Import row could not be serialized", exception);
    }
  }
}
