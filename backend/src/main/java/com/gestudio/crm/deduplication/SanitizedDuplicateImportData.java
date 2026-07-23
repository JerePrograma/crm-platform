package com.gestudio.crm.deduplication;

import com.gestudio.crm.prospect.ProspectApplicationService.CreateProspectCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

final class SanitizedDuplicateImportData {

  private static final int MAX_FIELD_LENGTH = 1_000;
  private static final int MAX_EVIDENCE_LENGTH = 4_000;

  private final String institutionName;
  private final String category;
  private final String locality;
  private final String province;
  private final String website;
  private final String email;
  private final String whatsapp;
  private final String source;
  private final String evidence;
  private final Integer priority;
  private final String administrativePain;
  private final Instant verifiedAt;

  private SanitizedDuplicateImportData(
      String institutionName,
      String category,
      String locality,
      String province,
      String website,
      String email,
      String whatsapp,
      String source,
      String evidence,
      Integer priority,
      String administrativePain,
      Instant verifiedAt) {
    this.institutionName = institutionName;
    this.category = category;
    this.locality = locality;
    this.province = province;
    this.website = website;
    this.email = email;
    this.whatsapp = whatsapp;
    this.source = source;
    this.evidence = evidence;
    this.priority = priority;
    this.administrativePain = administrativePain;
    this.verifiedAt = verifiedAt;
  }

  static SanitizedDuplicateImportData from(
      ObjectMapper objectMapper,
      String sourceData,
      String normalizedEmail,
      String normalizedPhone,
      String fallbackName) {
    Map<?, ?> raw = parse(objectMapper, sourceData);
    String institutionName = first(raw, "institucion");
    if (institutionName == null) {
      institutionName = bounded(fallbackName);
    }
    if (institutionName == null) {
      throw new IllegalArgumentException("Separate prospect name is required");
    }

    String importedEmail = first(raw, "correo publicado", "correo", "email");
    String importedPhone =
        first(raw, "telefono whatsapp", "telefono o whatsapp", "telefono", "whatsapp");
    String importedExternalId = first(raw, "id");
    String social = first(raw, "redes sociales");

    List<String> evidenceParts = new ArrayList<>();
    addEvidence(evidenceParts, first(raw, "validacion publicada"));
    addEvidence(evidenceParts, first(raw, "observaciones"));
    addEvidence(evidenceParts, first(raw, "observacion de control"));
    if (importedExternalId != null) {
      addEvidence(evidenceParts, "Identificador importado: " + importedExternalId);
    }
    if (social != null) {
      addEvidence(evidenceParts, "Red social: " + social);
    }

    return new SanitizedDuplicateImportData(
        institutionName,
        first(raw, "categoria"),
        first(raw, "localidad"),
        first(raw, "provincia"),
        first(raw, "sitio web"),
        importedEmail == null ? bounded(normalizedEmail) : importedEmail,
        importedPhone == null ? bounded(normalizedPhone) : importedPhone,
        first(raw, "fuente"),
        bounded(String.join(" | ", evidenceParts), MAX_EVIDENCE_LENGTH),
        priority(first(raw, "prioridad")),
        first(raw, "motivo de encaje"),
        date(first(raw, "fecha de verificacion")));
  }

  CreateProspectCommand toCommand(UUID reviewId) {
    return new CreateProspectCommand(
        institutionName,
        category,
        locality,
        province,
        "Argentina",
        website,
        null,
        "Contacto importado",
        email,
        null,
        whatsapp,
        "duplicate-review:" + reviewId,
        source == null ? "DUPLICATE_REVIEW" : source,
        evidence,
        null,
        priority,
        null,
        null,
        administrativePain,
        verifiedAt,
        null);
  }

  private static Map<?, ?> parse(ObjectMapper objectMapper, String sourceData) {
    if (sourceData == null || sourceData.isBlank()) {
      return Map.of();
    }
    try {
      Object parsed = objectMapper.readValue(sourceData, Map.class);
      return parsed instanceof Map<?, ?> map ? map : Map.of();
    } catch (RuntimeException exception) {
      return Map.of();
    }
  }

  private static String first(Map<?, ?> raw, String... aliases) {
    for (String alias : aliases) {
      Object value = raw.get(alias);
      if (value instanceof String stringValue) {
        String result = bounded(stringValue);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  private static Integer priority(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "alta" -> 1;
      case "media" -> 2;
      case "baja" -> 3;
      default -> {
        try {
          int numeric = Integer.parseInt(normalized);
          yield numeric >= 0 && numeric <= 5 ? numeric : null;
        } catch (NumberFormatException exception) {
          yield null;
        }
      }
    };
  }

  private static Instant date(String value) {
    if (value == null) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ignored) {
      // Continue with the date-only formats accepted by the importer.
    }
    for (DateTimeFormatter formatter :
        List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"))) {
      try {
        return LocalDate.parse(value, formatter).atStartOfDay().toInstant(ZoneOffset.UTC);
      } catch (DateTimeParseException ignored) {
        // Continue with the next supported format.
      }
    }
    return null;
  }

  private static void addEvidence(List<String> evidence, String value) {
    if (value != null && !evidence.contains(value)) {
      evidence.add(value);
    }
  }

  private static String bounded(String value) {
    return bounded(value, MAX_FIELD_LENGTH);
  }

  private static String bounded(String value, int maximum) {
    if (value == null) {
      return null;
    }
    String normalized = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
    if (normalized.isBlank()) {
      return null;
    }
    return normalized.length() <= maximum ? normalized : normalized.substring(0, maximum);
  }
}
