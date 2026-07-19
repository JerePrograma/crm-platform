package com.gestudio.crm.imports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

final class TestProspectWorkbookFactory {

  private static final String[] PROSPECT_HEADERS = {
    "ID",
    "Institución",
    "Localidad",
    "Provincia",
    "Categoría",
    "Sitio web",
    "Redes sociales",
    "Correo publicado",
    "Teléfono / WhatsApp",
    "Fuente",
    "Fecha de verificación",
    "Motivo de encaje",
    "Prioridad",
    "Estado comercial",
    "Fecha último contacto",
    "Validación Gmail",
    "Validación publicada",
    "Asunto sugerido",
    "Apertura personalizada",
    "Observaciones",
    "Auditoría operativa",
    "Cruce Gmail exacto",
    "Prueba técnica adjuntos",
    "Resultado envío",
    "Fecha auditoría",
    "Observación de control"
  };

  private TestProspectWorkbookFactory() {}

  static byte[] workbook(int prospectCount, int exclusionCount) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet prospects = workbook.createSheet("Prospectos");
      writeRow(prospects.createRow(0), PROSPECT_HEADERS);
      for (int index = 1; index <= prospectCount; index++) {
        Row row = prospects.createRow(index);
        row.createCell(0).setCellValue(index);
        row.createCell(1).setCellValue("Institución Fixture " + index);
        row.createCell(2).setCellValue("Localidad Fixture " + index);
        row.createCell(3).setCellValue("Provincia Fixture");
        row.createCell(4).setCellValue("Academia artística");
        row.createCell(5).setCellValue("https://fixture-" + index + ".example.test");
        row.createCell(6).setCellValue("No relevado");
        row.createCell(7).setCellValue("contacto" + index + "@example.test");
        row.createCell(8).setCellValue("+54 11 4000 " + String.format("%04d", index));
        row.createCell(9).setCellValue("fixture automatizada");
        row.createCell(10).setCellValue("2026-07-01");
        row.createCell(11).setCellValue("Gestión manual recurrente");
        row.createCell(12).setCellValue(index % 3 == 0 ? "Alta" : "Media");
        row.createCell(16).setCellValue("Correo ficticio validado");
        row.createCell(19).setCellValue("Dato de prueba sin destinatarios reales");
      }

      Sheet exclusions = workbook.createSheet("Exclusiones");
      writeRow(
          exclusions.createRow(0),
          new String[] {
            "Institución", "Correo", "Motivo de exclusión", "Fecha de verificación", "Resultado"
          });
      for (int index = 1; index <= exclusionCount; index++) {
        Row row = exclusions.createRow(index);
        row.createCell(0).setCellValue("Exclusión Fixture " + index);
        row.createCell(1).setCellValue("excluido" + index + "@example.test");
        row.createCell(2).setCellValue("Ya existe correo enviado en Gmail.");
        row.createCell(3).setCellValue("2026-07-01");
        row.createCell(4).setCellValue("No incorporar");
      }

      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Test workbook could not be generated", exception);
    }
  }

  private static void writeRow(Row row, String[] values) {
    for (int index = 0; index < values.length; index++) {
      row.createCell(index).setCellValue(values[index]);
    }
  }
}
