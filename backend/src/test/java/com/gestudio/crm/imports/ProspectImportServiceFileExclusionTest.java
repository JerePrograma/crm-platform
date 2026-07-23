package com.gestudio.crm.imports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gestudio.crm.imports.ImportJob.SourceType;
import com.gestudio.crm.imports.ProspectImportFileParser.ExclusionCandidate;
import com.gestudio.crm.imports.ProspectImportFileParser.ParsedImport;
import com.gestudio.crm.imports.ProspectImportFileParser.ProspectCandidate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProspectImportServiceFileExclusionTest {

  @Test
  void matchesSameFileExclusionsCaseInsensitivelyDuringPreviewPreparation() {
    ProspectCandidate prospect =
        new ProspectCandidate(
            2,
            Map.of(),
            "maestro:1",
            "Academia",
            null,
            null,
            null,
            null,
            null,
            " CONTACTO@EXAMPLE.TEST ",
            null,
            null,
            null,
            null,
            null,
            null);
    ExclusionCandidate exclusion =
        new ExclusionCandidate(
            2, Map.of(), "Academia", "contacto@example.test", "Existing conversation", null);
    ParsedImport parsed = new ParsedImport(SourceType.XLSX, List.of(prospect), List.of(exclusion));

    Set<String> exclusionEmails = ProspectImportService.fileExclusionEmails(parsed);

    assertTrue(ProspectImportService.isExcludedByImportFile(prospect, exclusionEmails));
  }

  @Test
  void doesNotExcludeProspectMissingFromSameFileExclusions() {
    ProspectCandidate prospect =
        new ProspectCandidate(
            2,
            Map.of(),
            "maestro:2",
            "Academia",
            null,
            null,
            null,
            null,
            null,
            "pendiente@example.test",
            null,
            null,
            null,
            null,
            null,
            null);

    assertFalse(ProspectImportService.isExcludedByImportFile(prospect, Set.of()));
  }
}
