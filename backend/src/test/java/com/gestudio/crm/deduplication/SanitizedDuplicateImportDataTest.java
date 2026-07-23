package com.gestudio.crm.deduplication;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SanitizedDuplicateImportDataTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void preservesSupportedImportedFieldsAndChannels() {
    String sourceData =
        """
        {
          "id":"ext-42",
          "institucion":"Academia Flores",
          "localidad":"Palermo",
          "provincia":"Buenos Aires",
          "categoria":"Danza",
          "sitio web":"https://flores.example",
          "correo publicado":"info@flores.example",
          "telefono whatsapp":"+54 9 11 5555 1212",
          "fuente":"Directorio público",
          "fecha de verificacion":"2026-07-20",
          "motivo de encaje":"Administra alumnos y cuotas",
          "prioridad":"alta",
          "observaciones":"Dato verificado",
          "redes sociales":"@flores",
          "campo desconocido":"no persistir como campo"
        }
        """;

    var command =
        SanitizedDuplicateImportData.from(
                objectMapper, sourceData, null, null, "Nombre alternativo")
            .toCommand(UUID.fromString("92bde240-479c-46f7-90b9-c5a06aa43e15"));

    assertThat(command.institutionName()).isEqualTo("Academia Flores");
    assertThat(command.locality()).isEqualTo("Palermo");
    assertThat(command.province()).isEqualTo("Buenos Aires");
    assertThat(command.category()).isEqualTo("Danza");
    assertThat(command.website()).isEqualTo("https://flores.example");
    assertThat(command.email()).isEqualTo("info@flores.example");
    assertThat(command.whatsapp()).isEqualTo("+54 9 11 5555 1212");
    assertThat(command.priority()).isEqualTo(1);
    assertThat(command.source()).isEqualTo("Directorio público");
    assertThat(command.verifiedAt()).isEqualTo(Instant.parse("2026-07-20T00:00:00Z"));
    assertThat(command.evidence())
        .contains("Dato verificado", "Identificador importado: ext-42", "Red social: @flores")
        .doesNotContain("campo desconocido");
    assertThat(command.externalSourceId()).startsWith("duplicate-review:");
  }

  @Test
  void fallsBackToNormalizedChannelsWhenRawEvidenceIsMalformed() {
    var command =
        SanitizedDuplicateImportData.from(
                objectMapper,
                "{not-json",
                "consulta@palermo.example",
                "+5491155550000",
                "Estudio Palermo")
            .toCommand(UUID.randomUUID());

    assertThat(command.institutionName()).isEqualTo("Estudio Palermo");
    assertThat(command.email()).isEqualTo("consulta@palermo.example");
    assertThat(command.whatsapp()).isEqualTo("+5491155550000");
  }
}
