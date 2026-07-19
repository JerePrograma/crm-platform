package com.gestudio.crm.imports;

import com.gestudio.crm.common.NormalizationService;
import com.gestudio.crm.imports.ImportJob.SourceType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class ProspectImportFileParser {

  private final NormalizationService normalizationService;
  private final DataFormatter dataFormatter = new DataFormatter(Locale.ROOT);

  public ProspectImportFileParser(NormalizationService normalizationService) {
    this.normalizationService = normalizationService;
  }

  public ParsedImport parse(String fileName, byte[] bytes) {
    if (fileName == null || fileName.isBlank() || bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("A non-empty import file is required");
    }
    String lowerCase = fileName.toLowerCase(Locale.ROOT);
    if (lowerCase.endsWith(".xlsx")) {
      return parseWorkbook(bytes);
    }
    if (lowerCase.endsWith(".csv")) {
      return parseCsv(bytes);
    }
    throw new IllegalArgumentException("Only .xlsx and .csv prospect imports are supported");
  }

  private ParsedImport parseWorkbook(byte[] bytes) {
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      Sheet prospectsSheet = workbook.getSheet("Prospectos");
      if (prospectsSheet == null && workbook.getNumberOfSheets() > 0) {
        prospectsSheet = workbook.getSheetAt(0);
      }
      if (prospectsSheet == null) {
        throw new IllegalArgumentException("The workbook does not contain a prospect sheet");
      }
      List<ProspectCandidate> prospects = parseProspectSheet(prospectsSheet);
      Sheet exclusionsSheet = workbook.getSheet("Exclusiones");
      List<ExclusionCandidate> exclusions =
          exclusionsSheet == null ? List.of() : parseExclusionSheet(exclusionsSheet);
      return new ParsedImport(SourceType.XLSX, prospects, exclusions);
    } catch (IOException exception) {
      throw new IllegalArgumentException("The XLSX file could not be read", exception);
    }
  }

  private List<ProspectCandidate> parseProspectSheet(Sheet sheet) {
    Row headerRow = sheet.getRow(sheet.getFirstRowNum());
    Map<String, Integer> headers = headers(headerRow);
    requireHeader(headers, "institucion");

    List<ProspectCandidate> candidates = new ArrayList<>();
    for (int index = headerRow.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
      Row row = sheet.getRow(index);
      if (row == null) {
        continue;
      }
      String institutionName = value(headers, row, "institucion");
      if (normalizationService.trimToNull(institutionName) == null) {
        continue;
      }
      Map<String, String> rawData = rawData(headers, row);
      candidates.add(
          new ProspectCandidate(
              row.getRowNum() + 1,
              rawData,
              value(headers, row, "id"),
              institutionName,
              value(headers, row, "localidad"),
              value(headers, row, "provincia"),
              value(headers, row, "categoria"),
              cleanPublished(value(headers, row, "sitio web")),
              cleanPublished(value(headers, row, "redes sociales")),
              cleanPublished(value(headers, row, "correo publicado", "correo", "email")),
              cleanPublished(
                  value(
                      headers,
                      row,
                      "telefono whatsapp",
                      "telefono o whatsapp",
                      "telefono",
                      "whatsapp")),
              value(headers, row, "fuente"),
              dateValue(headers, row, "fecha de verificacion"),
              value(headers, row, "motivo de encaje"),
              priority(value(headers, row, "prioridad")),
              joinEvidence(
                  value(headers, row, "validacion publicada"),
                  value(headers, row, "observaciones"),
                  value(headers, row, "observacion de control"))));
    }
    return List.copyOf(candidates);
  }

  private List<ExclusionCandidate> parseExclusionSheet(Sheet sheet) {
    Row headerRow = sheet.getRow(sheet.getFirstRowNum());
    Map<String, Integer> headers = headers(headerRow);
    requireHeader(headers, "correo");

    List<ExclusionCandidate> candidates = new ArrayList<>();
    for (int index = headerRow.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
      Row row = sheet.getRow(index);
      if (row == null) {
        continue;
      }
      String email = cleanPublished(value(headers, row, "correo", "email"));
      if (email == null) {
        continue;
      }
      candidates.add(
          new ExclusionCandidate(
              row.getRowNum() + 1,
              rawData(headers, row),
              value(headers, row, "institucion"),
              email,
              value(headers, row, "motivo de exclusion", "motivo"),
              dateValue(headers, row, "fecha de verificacion")));
    }
    return List.copyOf(candidates);
  }

  private ParsedImport parseCsv(byte[] bytes) {
    String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    List<List<String>> rows = parseCsvRows(content);
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("The CSV file is empty");
    }
    Map<String, Integer> headers = headers(rows.getFirst());
    requireHeader(headers, "institucion");
    List<ProspectCandidate> candidates = new ArrayList<>();
    for (int index = 1; index < rows.size(); index++) {
      List<String> row = rows.get(index);
      String institutionName = value(headers, row, "institucion");
      if (normalizationService.trimToNull(institutionName) == null) {
        continue;
      }
      candidates.add(
          new ProspectCandidate(
              index + 1,
              rawData(headers, row),
              value(headers, row, "id"),
              institutionName,
              value(headers, row, "localidad"),
              value(headers, row, "provincia"),
              value(headers, row, "categoria"),
              cleanPublished(value(headers, row, "sitio web")),
              cleanPublished(value(headers, row, "redes sociales")),
              cleanPublished(value(headers, row, "correo publicado", "correo", "email")),
              cleanPublished(value(headers, row, "telefono whatsapp", "telefono")),
              value(headers, row, "fuente"),
              parseDate(value(headers, row, "fecha de verificacion")),
              value(headers, row, "motivo de encaje"),
              priority(value(headers, row, "prioridad")),
              joinEvidence(
                  value(headers, row, "validacion publicada"),
                  value(headers, row, "observaciones"),
                  value(headers, row, "observacion de control"))));
    }
    return new ParsedImport(SourceType.CSV, List.copyOf(candidates), List.of());
  }

  private Map<String, Integer> headers(Row row) {
    if (row == null) {
      throw new IllegalArgumentException("The import header row is missing");
    }
    Map<String, Integer> headers = new LinkedHashMap<>();
    for (Cell cell : row) {
      String key = normalizationService.normalizeText(dataFormatter.formatCellValue(cell));
      if (key != null) {
        headers.put(key, cell.getColumnIndex());
      }
    }
    return headers;
  }

  private Map<String, Integer> headers(List<String> row) {
    Map<String, Integer> headers = new LinkedHashMap<>();
    for (int index = 0; index < row.size(); index++) {
      String key = normalizationService.normalizeText(row.get(index));
      if (key != null) {
        headers.put(key, index);
      }
    }
    return headers;
  }

  private void requireHeader(Map<String, Integer> headers, String name) {
    if (!headers.containsKey(name)) {
      throw new IllegalArgumentException("Required import column is missing: " + name);
    }
  }

  private String value(Map<String, Integer> headers, Row row, String... aliases) {
    for (String alias : aliases) {
      Integer index = headers.get(alias);
      if (index != null) {
        return normalizationService.trimToNull(dataFormatter.formatCellValue(row.getCell(index)));
      }
    }
    return null;
  }

  private String value(Map<String, Integer> headers, List<String> row, String... aliases) {
    for (String alias : aliases) {
      Integer index = headers.get(alias);
      if (index != null && index < row.size()) {
        return normalizationService.trimToNull(row.get(index));
      }
    }
    return null;
  }

  private Instant dateValue(Map<String, Integer> headers, Row row, String alias) {
    Integer index = headers.get(alias);
    if (index == null) {
      return null;
    }
    Cell cell = row.getCell(index);
    if (cell == null) {
      return null;
    }
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
      return DateUtil.getJavaDate(cell.getNumericCellValue()).toInstant();
    }
    return parseDate(dataFormatter.formatCellValue(cell));
  }

  private Instant parseDate(String value) {
    String normalized = normalizationService.trimToNull(value);
    if (normalized == null) {
      return null;
    }
    for (DateTimeFormatter formatter :
        List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"))) {
      try {
        return LocalDate.parse(normalized, formatter).atStartOfDay().toInstant(ZoneOffset.UTC);
      } catch (DateTimeParseException ignored) {
        // Try the next supported format.
      }
    }
    return null;
  }

  private Map<String, String> rawData(Map<String, Integer> headers, Row row) {
    Map<String, String> values = new LinkedHashMap<>();
    headers.forEach(
        (header, index) -> values.put(header, dataFormatter.formatCellValue(row.getCell(index))));
    return Map.copyOf(values);
  }

  private Map<String, String> rawData(Map<String, Integer> headers, List<String> row) {
    Map<String, String> values = new LinkedHashMap<>();
    headers.forEach(
        (header, index) -> values.put(header, index < row.size() ? row.get(index) : ""));
    return Map.copyOf(values);
  }

  private String cleanPublished(String value) {
    String normalized = normalizationService.trimToNull(value);
    if (normalized == null) {
      return null;
    }
    String key = normalizationService.normalizeText(normalized);
    if (key == null
        || key.startsWith("no publicado")
        || key.startsWith("no relevado")
        || key.startsWith("sin dato")
        || key.equals("n a")) {
      return null;
    }
    return normalized;
  }

  private Integer priority(String value) {
    String normalized = normalizationService.normalizeText(value);
    if (normalized == null) {
      return null;
    }
    return switch (normalized) {
      case "alta" -> 1;
      case "media" -> 2;
      case "baja" -> 3;
      default -> null;
    };
  }

  private String joinEvidence(String... values) {
    return java.util.Arrays.stream(values)
        .map(normalizationService::trimToNull)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .collect(java.util.stream.Collectors.joining(" | "));
  }

  private List<List<String>> parseCsvRows(String content) {
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    for (int index = 0; index < content.length(); index++) {
      char character = content.charAt(index);
      if (character == '"') {
        if (quoted && index + 1 < content.length() && content.charAt(index + 1) == '"') {
          field.append('"');
          index++;
        } else {
          quoted = !quoted;
        }
      } else if (character == ',' && !quoted) {
        row.add(field.toString());
        field.setLength(0);
      } else if ((character == '\n' || character == '\r') && !quoted) {
        if (character == '\r' && index + 1 < content.length() && content.charAt(index + 1) == '\n') {
          index++;
        }
        row.add(field.toString());
        field.setLength(0);
        if (row.stream().anyMatch(value -> !value.isBlank())) {
          rows.add(List.copyOf(row));
        }
        row.clear();
      } else {
        field.append(character);
      }
    }
    row.add(field.toString());
    if (row.stream().anyMatch(value -> !value.isBlank())) {
      rows.add(List.copyOf(row));
    }
    return List.copyOf(rows);
  }

  public record ParsedImport(
      SourceType sourceType,
      List<ProspectCandidate> prospects,
      List<ExclusionCandidate> exclusions) {}

  public record ProspectCandidate(
      int rowNumber,
      Map<String, String> rawData,
      String externalId,
      String institutionName,
      String locality,
      String province,
      String category,
      String website,
      String social,
      String email,
      String phoneOrWhatsapp,
      String source,
      Instant verifiedAt,
      String administrativePain,
      Integer priority,
      String evidence) {}

  public record ExclusionCandidate(
      int rowNumber,
      Map<String, String> rawData,
      String institutionName,
      String email,
      String reason,
      Instant verifiedAt) {}
}
