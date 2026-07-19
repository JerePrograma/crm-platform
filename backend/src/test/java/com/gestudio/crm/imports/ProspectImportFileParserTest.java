package com.gestudio.crm.imports;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestudio.crm.common.NormalizationService;
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
}
