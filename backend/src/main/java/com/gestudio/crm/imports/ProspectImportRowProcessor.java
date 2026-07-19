package com.gestudio.crm.imports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.contact.ContactChannelType;
import com.gestudio.crm.exclusion.Exclusion;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProspectImportRowProcessor {

  private final ImportJobRepository importJobRepository;
  private final ImportRowRepository importRowRepository;
  private final DuplicateReviewRepository duplicateReviewRepository;
  private final ExclusionRepository exclusionRepository;
  private final ProspectRepository prospectRepository;
  private final ProspectDeduplicationService deduplicationService;
  private final ProspectApplicationService prospectApplicationService;
  private final NormalizationService normalizationService;
  private final ObjectMapper objectMapper;

  public ProspectImportRowProcessor(
      ImportJobRepository importJobRepository,
      ImportRowRepository importRowRepository,
      DuplicateReviewRepository duplicateReviewRepository,
      ExclusionRepository exclusionRepository,
      ProspectRepository prospectRepository,
      ProspectDeduplicationService deduplicationService,
      ProspectApplicationService prospectApplicationService,
      NormalizationService normalizationService,
      ObjectMapper objectMapper) {
    this.importJobRepository = importJobRepository;
    this.importRowRepository = importRowRepository;
    this.duplicateReviewRepository = duplicateReviewRepository;
    this.exclusionRepository = exclusionRepository;
    this.prospectRepository = prospectRepository;
    this.deduplicationService = deduplicationService;
    this.prospectApplicationService = prospectApplicationService;
    this.normalizationService = normalizationService;
    this.objectMapper = objectMapper;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RowOutcome processProspect(UUID jobId, ProspectCandidate candidate, boolean dryRun) {
    ImportJob job = getJob(jobId);
    String normalizedEmail = normalizationService.normalizeEmail(candidate.email());
    String normalizedPhone = normalizationService.normalizePhone(candidate.phoneOrWhatsapp());
    ImportRow row =
        importRowRepository.save(
            ImportRow.create(
                job,
                "Prospectos",
                candidate.rowNumber(),
                rawJson(candidate.rawData()),
                normalizedEmail,
                normalizedPhone));

    if (normalizationService.trimToNull(candidate.institutionName()) == null) {
      row.reject("Institution name is required");
      return RowOutcome.REJECTED;
    }

    DeduplicationOutcome deduplication = deduplicationService.evaluate(candidate);
    if (deduplication.kind() == Kind.EXACT_DUPLICATE) {
      row.markDuplicate();
      return RowOutcome.DUPLICATE;
    }
    if (deduplication.kind() == Kind.REVIEW_REQUIRED) {
      row.requireReview();
      if (!dryRun) {
        duplicateReviewRepository.save(
            DuplicateReview.create(
                row,
                deduplication.existingProspect(),
                deduplication.matchType(),
                BigDecimal.valueOf(deduplication.confidence())
                    .setScale(4, RoundingMode.HALF_UP),
                "Ambiguous nominal match; automatic merge is forbidden"));
      }
      return RowOutcome.REVIEW_REQUIRED;
    }

    if (dryRun) {
      row.accept(null);
      return RowOutcome.ACCEPTED;
    }

    ProspectView created = prospectApplicationService.create(toCommand(candidate));
    Prospect prospect =
        prospectRepository
            .findById(created.id())
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
        importRowRepository.save(
            ImportRow.create(
                job,
                "Exclusiones",
                candidate.rowNumber(),
                rawJson(candidate.rawData()),
                normalizedEmail,
                null));
    if (normalizedEmail == null) {
      row.reject("Exclusion email is required");
      return RowOutcome.REJECTED;
    }
    if (exclusionRepository.existsByChannelTypeAndNormalizedValue(
        ContactChannelType.EMAIL, normalizedEmail)) {
      row.markDuplicate();
      return RowOutcome.DUPLICATE;
    }
    if (!dryRun) {
      exclusionRepository.save(
          Exclusion.create(
              ContactChannelType.EMAIL, normalizedEmail, mapReason(candidate.reason())));
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
            normalizationService.normalizeEmail(candidate.email()),
            normalizationService.normalizePhone(candidate.phoneOrWhatsapp()));
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
            normalizationService.normalizeEmail(candidate.email()),
            null);
    row.reject(message);
    importRowRepository.save(row);
  }

  private ImportJob getJob(UUID jobId) {
    return importJobRepository
        .findById(jobId)
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

  private String rawJson(java.util.Map<String, String> rawData) {
    try {
      return objectMapper.writeValueAsString(rawData);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Import row could not be serialized", exception);
    }
  }
}
