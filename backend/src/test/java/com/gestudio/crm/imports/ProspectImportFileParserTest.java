package com.gestudio.crm.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gestudio.crm.common.NormalizationService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ProspectImportFileParserTest {

  private final ProspectImportFileParser parser =
      new ProspectImportFileParser(new NormalizationService());

  @Test
  void parsesOneHundredProspectsAndSixteenExclusionsByHeader() {
    ProspectImportFileParser.ParsedImport parsed =
        parser.parse("prospectos-fixture.xlsx", TestProspectWorkbookFactory.workbook(100, 16));

    assertThat(parsed.prospects()).hasSize(100);
    assertThat(parsed.exclusions()).hasSize(16);
    assertThat(parsed.prospects().getFirst().institutionName())
        .isEqualTo("Institución Fixture 1");
    assertThat(parsed.prospects().getFirst().social()).isNull();
    assertThat(parsed.exclusions().getFirst().email())
        .isEqualTo("excluido1@example.test");
  }

  @Test
  void detectsSemicolonDelimitedCsvAndPreservesQuotedDelimiters() {
    byte[] csv =
        ("Institución;Correo publicado;Observaciones\n"
                + "Academia Fixture;contacto@example.test;\"Usa planillas; cuadernos y WhatsApp\"\n")
            .getBytes(StandardCharsets.UTF_8);

    ProspectImportFileParser.ParsedImport parsed = parser.parse("fixture.csv", csv);

    assertThat(parsed.prospects()).singleElement();
    assertThat(parsed.prospects().getFirst().institutionName()).isEqualTo("Academia Fixture");
    assertThat(parsed.prospects().getFirst().evidence())
        .isEqualTo("Usa planillas; cuadernos y WhatsApp");
  }

  @Test
  void rejectsCsvWithUnclosedQuotedField() {
    byte[] csv =
        ("Institución,Observaciones\nAcademia Fixture,\"Texto sin cierre\n")
            .getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> parser.parse("invalid.csv", csv))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unclosed quoted field");
  }

  @Test
  void rejectsDuplicateHeadersAfterNormalization() {
    byte[] csv =
        ("Institución,INSTITUCION,Correo publicado\n"
                + "Academia Uno,Academia Dos,contacto@example.test\n")
            .getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> parser.parse("duplicate-headers.csv", csv))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate normalized import column");
  }
}
