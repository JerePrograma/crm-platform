package com.gestudio.crm.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsvSafetyTest {

  @ParameterizedTest
  @ValueSource(strings = {"=SUM(A1:A2)", "+1", "-1", "@cmd", "\tcommand", "\rcommand"})
  void neutralizesSpreadsheetFormulaPrefixes(String value) {
    assertThat(CsvSafety.cell(value)).startsWith("\"'").endsWith("\"");
  }
}
