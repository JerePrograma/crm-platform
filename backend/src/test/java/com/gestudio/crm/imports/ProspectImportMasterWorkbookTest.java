package com.gestudio.crm.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gestudio.crm.common.NormalizationService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ProspectImportMasterWorkbookTest {

  private final ProspectImportFileParser parser =
      new ProspectImportFileParser(new NormalizationService());

  @Test
  void parsesGestudioMasterHeadersPrefixesIdsAndDerivesExistingConversationExclusions() {
    ProspectImportFileParser.ParsedImport parsed =
        parser.parse("gestudio-master.xlsx", gestudioMasterWorkbook());

    assertEquals(2, parsed.prospects().size());
    assertEquals(1, parsed.exclusions().size());

    ProspectImportFileParser.ProspectCandidate contacted = parsed.prospects().getFirst();
    assertEquals("maestro:1", contacted.externalId());
    assertEquals("Academia Contactada", contacted.institutionName());
    assertEquals("contactada@example.test", contacted.email());
    assertEquals("Campaña Gmail", contacted.source());
    assertEquals(Integer.valueOf(1), contacted.priority());
    assertTrue(contacted.evidence().contains("Interesado"));
    assertTrue(contacted.evidence().contains("alternativo@example.test"));

    ProspectImportFileParser.ExclusionCandidate exclusion = parsed.exclusions().getFirst();
    assertEquals("contactada@example.test", exclusion.email());
    assertTrue(exclusion.reason().contains("Ya existe conversación"));
  }

  @Test
  void prefersCanonicalProspectosWhenBothSupportedSheetsExist() {
    ProspectImportFileParser.ParsedImport parsed =
        parser.parse("both.xlsx", workbookWithBothSupportedSheets());

    assertEquals(1, parsed.prospects().size());
    assertEquals("Prospectos elegidos", parsed.prospects().getFirst().institutionName());
    assertEquals("legacy-1", parsed.prospects().getFirst().externalId());
    assertEquals(0, parsed.exclusions().size());
  }

  @Test
  void rejectsWorkbookWithoutSupportedProspectSheet() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> parser.parse("unsupported.xlsx", unsupportedWorkbook()));

    assertTrue(exception.getMessage().contains("'Prospectos' or 'Maestro'"));
  }

  private byte[] gestudioMasterWorkbook() {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      workbook.createSheet("Resumen").createRow(0).createCell(0).setCellValue("No importar");

      Sheet master = workbook.createSheet("Maestro");
      writeRow(
          master.createRow(0),
          "ID",
          "Institución",
          "Correo principal",
          "Correos alternativos",
          "Localidad",
          "Provincia",
          "Categoría",
          "Origen",
          "Fuente / evidencia",
          "Estado comercial",
          "Prioridad",
          "Primer contacto",
          "Último contacto",
          "Mensajes salientes",
          "Respondió",
          "Resultado de respuesta",
          "Entrega / rebote",
          "Teléfono / WhatsApp",
          "Adjuntos",
          "Próxima acción",
          "Último asunto",
          "Señales / etiquetas",
          "Observaciones",
          "Thread ID",
          "Message IDs",
          "Lote");

      Row contacted = master.createRow(1);
      contacted.createCell(0).setCellValue(1);
      contacted.createCell(1).setCellValue("Academia Contactada");
      contacted.createCell(2).setCellValue("contactada@example.test");
      contacted.createCell(3).setCellValue("alternativo@example.test");
      contacted.createCell(4).setCellValue("Junín");
      contacted.createCell(5).setCellValue("Buenos Aires");
      contacted.createCell(6).setCellValue("Estudio de danza");
      contacted.createCell(7).setCellValue("Campaña Gmail");
      contacted.createCell(8).setCellValue("Auditoría de Gmail");
      contacted.createCell(9).setCellValue("Interesado");
      contacted.createCell(10).setCellValue("Alta");
      contacted
          .createCell(11)
          .setCellValue(
              Date.from(LocalDate.of(2026, 7, 6).atStartOfDay().toInstant(ZoneOffset.UTC)));
      contacted
          .createCell(12)
          .setCellValue(
              Date.from(LocalDate.of(2026, 7, 20).atStartOfDay().toInstant(ZoneOffset.UTC)));
      contacted.createCell(13).setCellValue(4);
      contacted.createCell(14).setCellValue("Sí");
      contacted.createCell(15).setCellValue("Respondió con interés");
      contacted.createCell(16).setCellValue("Enviado");
      contacted.createCell(17).setCellValue("+54 11 4000 0001");
      contacted.createCell(19).setCellValue("Ofrecer acceso de prueba");
      contacted.createCell(20).setCellValue("Re: Gestudio");
      contacted.createCell(21).setCellValue("Gestudio/Respondieron");
      contacted.createCell(22).setCellValue("Seguimiento por WhatsApp");
      contacted.createCell(25).setCellValue("Primera ola");

      Row pending = master.createRow(2);
      pending.createCell(0).setCellValue(2);
      pending.createCell(1).setCellValue("Academia Pendiente");
      pending.createCell(2).setCellValue("pendiente@example.test");
      pending.createCell(9).setCellValue("Pendiente");
      pending.createCell(10).setCellValue("Media");

      Sheet exclusions = workbook.createSheet("Exclusiones previas");
      writeRow(
          exclusions.createRow(0),
          "Institución",
          "Correo",
          "Motivo de exclusión",
          "Fecha de verificación",
          "Resultado");
      Row exclusion = exclusions.createRow(1);
      exclusion.createCell(0).setCellValue("Academia Contactada");
      exclusion.createCell(1).setCellValue("contactada@example.test");
      exclusion.createCell(2).setCellValue("Ya existe conversación y seguimiento cerrado.");
      exclusion.createCell(3).setCellValue("2026-07-22");
      exclusion.createCell(4).setCellValue("No incorporar");

      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not create master workbook fixture", exception);
    }
  }

  private byte[] workbookWithBothSupportedSheets() {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet master = workbook.createSheet("Maestro");
      writeRow(master.createRow(0), "ID", "Institución", "Correo principal", "Primer contacto");
      Row masterRow = master.createRow(1);
      masterRow.createCell(0).setCellValue(1);
      masterRow.createCell(1).setCellValue("Maestro no elegido");
      masterRow.createCell(2).setCellValue("maestro@example.test");
      masterRow.createCell(3).setCellValue("2026-07-01");

      Sheet prospects = workbook.createSheet("Prospectos");
      writeRow(prospects.createRow(0), "ID", "Institución", "Correo publicado");
      Row prospectRow = prospects.createRow(1);
      prospectRow.createCell(0).setCellValue("legacy-1");
      prospectRow.createCell(1).setCellValue("Prospectos elegidos");
      prospectRow.createCell(2).setCellValue("prospectos@example.test");

      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not create supported workbook fixture", exception);
    }
  }

  private byte[] unsupportedWorkbook() {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      workbook.createSheet("Resumen").createRow(0).createCell(0).setCellValue("Sin prospectos");
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not create unsupported workbook fixture", exception);
    }
  }

  private void writeRow(Row row, String... values) {
    for (int index = 0; index < values.length; index++) {
      row.createCell(index).setCellValue(values[index]);
    }
  }
}
