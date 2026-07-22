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
    assertThat(parsed.prospects().getFirst().institutionName()).isEqualTo("Institución Fixture 1");
    assertThat(parsed.prospects().getFirst().social()).isNull();
    assertThat(parsed.exclusions().getFirst().email()).isEqualTo("excluido1@example.test");
  }

  @Test
  void previewsSyntheticDatasetsAtOperationalSizes() {
    for (int rows : new int[] {100, 1_000, 10_000}) {
      byte[] workbook = TestProspectWorkbookFactory.workbook(rows, 0);
      long started = System.nanoTime();

      ProspectImportFileParser.ParsedImport parsed =
          parser.parse("synthetic-performance.xlsx", workbook);

      long durationMillis = (System.nanoTime() - started) / 1_000_000;
      assertThat(parsed.prospects()).hasSize(rows);
      System.out.printf(
          "SYNTHETIC_IMPORT_PREVIEW rows=%d bytes=%d durationMs=%d%n",
          rows, workbook.length, durationMillis);
    }
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

  @Test
  void rejectsMismatchedBinaryContentAndOversizedCells() {
    assertThatThrownBy(
            () -> parser.parse("fake.xlsx", "not-a-zip".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match");
    assertThatThrownBy(
            () ->
                parser.parse(
                    "binary.csv",
                    new byte[] {'I', 'n', 's', 't', 'i', 't', 'u', 'c', 'i', 'o', 'n', 0}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("binary data");
    byte[] oversizedCell =
        ("Institución,Observaciones\nAcademia," + "x".repeat(10_001))
            .getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> parser.parse("oversized.csv", oversizedCell))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("10000 characters");
  }
}
