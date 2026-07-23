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
import java.util.TimeZone;
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

  private static final String PROSPECTS_SHEET = "Prospectos";
  private static final String MASTER_SHEET = "Maestro";
  private static final String EXCLUSIONS_SHEET = "Exclusiones";
  private static final String PREVIOUS_EXCLUSIONS_SHEET = "Exclusiones previas";
  private static final String MASTER_EXTERNAL_ID_PREFIX = "maestro:";

  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
  private static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
  private static final int MAX_DATA_ROWS = 10_000;
  private static final int MAX_COLUMNS = 100;
  private static final int MAX_CELL_CHARACTERS = 10_000;

  private final NormalizationService normalizationService;
  private final DataFormatter dataFormatter = new DataFormatter(Locale.ROOT);

  public ProspectImportFileParser(NormalizationService normalizationService) {
    this.normalizationService = normalizationService;
  }

  public ParsedImport parse(String fileName, byte[] bytes) {
    if (fileName == null || fileName.isBlank() || bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("A non-empty import file is required");
    }
    if (bytes.length > MAX_FILE_BYTES) {
      throw new IllegalArgumentException("Import file exceeds the 10 MiB application limit");
    }
    String lowerCase = fileName.toLowerCase(Locale.ROOT);
    if (lowerCase.endsWith(".xlsx")) {
      if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
        throw new IllegalArgumentException("The XLSX content does not match its file extension");
      }
      return parseWorkbook(bytes);
    }
    if (lowerCase.endsWith(".csv")) {
      for (byte value : bytes) {
        if (value == 0) {
          throw new IllegalArgumentException("CSV imports must be UTF-8 text without binary data");
        }
      }
      return parseCsv(bytes);
    }
    throw new IllegalArgumentException("Only .xlsx and .csv prospect imports are supported");
  }

  private ParsedImport parseWorkbook(byte[] bytes) {
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      Sheet prospectsSheet = supportedProspectSheet(workbook);
      List<ProspectCandidate> prospects = parseProspectSheet(prospectsSheet);

      Sheet exclusionsSheet = workbook.getSheet(EXCLUSIONS_SHEET);
      if (exclusionsSheet == null) {
        exclusionsSheet = workbook.getSheet(PREVIOUS_EXCLUSIONS_SHEET);
      }

      List<ExclusionCandidate> declaredExclusions =
          exclusionsSheet == null ? List.of() : parseExclusionSheet(exclusionsSheet);
      List<ExclusionCandidate> derivedMasterExclusions =
          MASTER_SHEET.equals(prospectsSheet.getSheetName())
              ? parseMasterExclusions(prospectsSheet)
              : List.of();

      return new ParsedImport(
          SourceType.XLSX,
          prospects,
          mergeExclusions(declaredExclusions, derivedMasterExclusions));
    } catch (IOException exception) {
      throw new IllegalArgumentException("The XLSX file could not be read", exception);
    }
  }

  private Sheet supportedProspectSheet(Workbook workbook) {
    Sheet prospectsSheet = workbook.getSheet(PROSPECTS_SHEET);
    if (prospectsSheet != null) {
      return prospectsSheet;
    }
    Sheet masterSheet = workbook.getSheet(MASTER_SHEET);
    if (masterSheet != null) {
      return masterSheet;
    }
    throw new IllegalArgumentException(
        "The workbook must contain a 'Prospectos' or 'Maestro' sheet");
  }

  private List<ProspectCandidate> parseProspectSheet(Sheet sheet) {
    Row headerRow = sheet.getRow(sheet.getFirstRowNum());
    Map<String, Integer> headers = headers(headerRow);
    requireHeader(headers, "institucion");

    boolean masterSheet = MASTER_SHEET.equals(sheet.getSheetName());
    List<ProspectCandidate> candidates = new ArrayList<>();
    if (sheet.getLastRowNum() - headerRow.getRowNum() > MAX_DATA_ROWS) {
      throw new IllegalArgumentException("Import contains more than 10000 prospect rows");
    }
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
              externalId(masterSheet, value(headers, row, "id")),
              institutionName,
              value(headers, row, "localidad"),
              value(headers, row, "provincia"),
              value(headers, row, "categoria"),
              cleanPublished(value(headers, row, "sitio web")),
              cleanPublished(value(headers, row, "redes sociales")),
              cleanPublished(
                  value(
                      headers,
                      row,
                      "correo publicado",
                      "correo principal",
                      "correo",
                      "email")),
              cleanPublished(
                  value(
                      headers,
                      row,
                      "telefono whatsapp",
                      "telefono o whatsapp",
                      "telefono",
                      "whatsapp")),
              value(headers, row, "fuente", "origen"),
              dateValue(headers, row, "fecha de verificacion", "fecha auditoria"),
              value(headers, row, "motivo de encaje"),
              priority(value(headers, row, "prioridad")),
              joinEvidence(
                  value(headers, row, "fuente evidencia"),
                  value(headers, row, "validacion publicada"),
                  value(headers, row, "correos alternativos"),
                  value(headers, row, "estado comercial"),
                  value(headers, row, "primer contacto"),
                  value(headers, row, "ultimo contacto"),
                  value(headers, row, "mensajes salientes"),
                  value(headers, row, "respondio"),
                  value(headers, row, "resultado de respuesta"),
                  value(headers, row, "entrega rebote"),
                  value(headers, row, "adjuntos"),
                  value(headers, row, "proxima accion"),
                  value(headers, row, "ultimo asunto"),
                  value(headers, row, "senales etiquetas"),
                  value(headers, row, "observaciones"),
                  value(headers, row, "observacion de control"),
                  value(headers, row, "lote"))));
    }
    return List.copyOf(candidates);
  }

  private List<ExclusionCandidate> parseMasterExclusions(Sheet sheet) {
    Row headerRow = sheet.getRow(sheet.getFirstRowNum());
    Map<String, Integer> headers = headers(headerRow);
    List<ExclusionCandidate> candidates = new ArrayList<>();

    for (int index = headerRow.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
      Row row = sheet.getRow(index);
      if (row == null) {
        continue;
      }
      String email =
          cleanPublished(value(headers, row, "correo principal", "correo publicado", "correo"));
      if (email == null) {
        continue;
      }
      String firstContact = value(headers, row, "primer contacto");
      String commercialStatus = value(headers, row, "estado comercial");
      String delivery = value(headers, row, "entrega rebote");
      if (!requiresMasterExclusion(firstContact, commercialStatus, delivery)) {
        continue;
      }
      candidates.add(
          new ExclusionCandidate(
              row.getRowNum() + 1,
              rawData(headers, row),
              value(headers, row, "institucion"),
              email,
              masterExclusionReason(commercialStatus, delivery),
              dateValue(headers, row, "ultimo contacto", "primer contacto")));
    }
    return List.copyOf(candidates);
  }

  private boolean requiresMasterExclusion(
      String firstContact, String commercialStatus, String delivery) {
    if (normalizationService.trimToNull(firstContact) != null) {
      return true;
    }
    String status = normalizationService.normalizeText(commercialStatus);
    if (status != null
        && (status.contains("no contactar")
            || status.contains("cerrado")
            || status.contains("respondio")
            || status.contains("interesado")
            || status.contains("seguimiento")
            || status.contains("rebote")
            || status.contains("invalido")
            || status.contains("cliente"))) {
      return true;
    }
    String deliveryStatus = normalizationService.normalizeText(delivery);
    return deliveryStatus != null
        && (deliveryStatus.contains("rebote")
            || deliveryStatus.contains("invalido")
            || deliveryStatus.contains("no existe"));
  }

  private String masterExclusionReason(String commercialStatus, String delivery) {
    return joinEvidence(
        "Existing conversation or non-contactable record from Gestudio master",
        commercialStatus,
        delivery);
  }

  private List<ExclusionCandidate> mergeExclusions(
      List<ExclusionCandidate> declared, List<ExclusionCandidate> derived) {
    Map<String, ExclusionCandidate> exclusionsByEmail = new LinkedHashMap<>();
    for (ExclusionCandidate candidate : declared) {
      exclusionsByEmail.putIfAbsent(exclusionKey(candidate.email()), candidate);
    }
    for (ExclusionCandidate candidate : derived) {
      exclusionsByEmail.putIfAbsent(exclusionKey(candidate.email()), candidate);
    }
    return List.copyOf(exclusionsByEmail.values());
  }

  private String exclusionKey(String email) {
    String normalized = normalizationService.trimToNull(email);
    return normalized == null ? "" : normalized.toLowerCase(Locale.ROOT);
  }

  private String externalId(boolean masterSheet, String value) {
    String normalized = normalizationService.trimToNull(value);
    if (normalized == null || !masterSheet || normalized.startsWith(MASTER_EXTERNAL_ID_PREFIX)) {
      return normalized;
    }
    return MASTER_EXTERNAL_ID_PREFIX + normalized;
  }

  private List<ExclusionCandidate> parseExclusionSheet(Sheet sheet) {
    Row headerRow = sheet.getRow(sheet.getFirstRowNum());
    Map<String, Integer> headers = headers(headerRow);
    requireHeader(headers, "correo");

    List<ExclusionCandidate> candidates = new ArrayList<>();
    if (sheet.getLastRowNum() - headerRow.getRowNum() > MAX_DATA_ROWS) {
      throw new IllegalArgumentException("Import contains more than 10000 exclusion rows");
    }
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
    List<List<String>> rows = parseCsvRows(content, detectDelimiter(content));
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("The CSV file is empty");
    }
    if (rows.size() - 1 > MAX_DATA_ROWS) {
      throw new IllegalArgumentException("Import contains more than 10000 prospect rows");
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
              cleanPublished(
                  value(
                      headers,
                      row,
                      "correo publicado",
                      "correo principal",
                      "correo",
                      "email")),
              cleanPublished(
                  value(
                      headers,
                      row,
                      "telefono whatsapp",
                      "telefono o whatsapp",
                      "telefono",
                      "whatsapp")),
              value(headers, row, "fuente", "origen"),
              parseDate(value(headers, row, "fecha de verificacion", "fecha auditoria")),
              value(headers, row, "motivo de encaje"),
              priority(value(headers, row, "prioridad")),
              joinEvidence(
                  value(headers, row, "fuente evidencia"),
                  value(headers, row, "validacion publicada"),
                  value(headers, row, "correos alternativos"),
                  value(headers, row, "estado comercial"),
                  value(headers, row, "primer contacto"),
                  value(headers, row, "ultimo contacto"),
                  value(headers, row, "mensajes salientes"),
                  value(headers, row, "respondio"),
                  value(headers, row, "resultado de respuesta"),
                  value(headers, row, "entrega rebote"),
                  value(headers, row, "adjuntos"),
                  value(headers, row, "proxima accion"),
                  value(headers, row, "ultimo asunto"),
                  value(headers, row, "senales etiquetas"),
                  value(headers, row, "observaciones"),
                  value(headers, row, "observacion de control"),
                  value(headers, row, "lote"))));
    }
    return new ParsedImport(SourceType.CSV, List.copyOf(candidates), List.of());
  }

  private Map<String, Integer> headers(Row row) {
    if (row == null) {
      throw new IllegalArgumentException("The import header row is missing");
    }
    Map<String, Integer> headers = new LinkedHashMap<>();
    if (row.getLastCellNum() > MAX_COLUMNS) {
      throw new IllegalArgumentException("Import contains more than 100 columns");
    }
    for (Cell cell : row) {
      String key = normalizationService.normalizeText(dataFormatter.formatCellValue(cell));
      putHeader(headers, key, cell.getColumnIndex());
    }
    return headers;
  }

  private Map<String, Integer> headers(List<String> row) {
    if (row.size() > MAX_COLUMNS) {
      throw new IllegalArgumentException("Import contains more than 100 columns");
    }
    Map<String, Integer> headers = new LinkedHashMap<>();
    for (int index = 0; index < row.size(); index++) {
      String key = normalizationService.normalizeText(row.get(index));
      putHeader(headers, key, index);
    }
    return headers;
  }

  private void putHeader(Map<String, Integer> headers, String key, int index) {
    if (key == null) {
      return;
    }
    if (headers.putIfAbsent(key, index) != null) {
      throw new IllegalArgumentException("Duplicate normalized import column: " + key);
    }
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
        return checked(dataFormatter.formatCellValue(row.getCell(index)));
      }
    }
    return null;
  }

  private String value(Map<String, Integer> headers, List<String> row, String... aliases) {
    for (String alias : aliases) {
      Integer index = headers.get(alias);
      if (index != null && index < row.size()) {
        return checked(row.get(index));
      }
    }
    return null;
  }

  private Instant dateValue(Map<String, Integer> headers, Row row, String... aliases) {
    for (String alias : aliases) {
      Integer index = headers.get(alias);
      if (index == null) {
        continue;
      }
      Cell cell = row.getCell(index);
      if (cell == null) {
        continue;
      }
      if (cell.getCellType() == CellType.NUMERIC
          && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
        return DateUtil.getJavaDate(cell.getNumericCellValue(), false, UTC).toInstant();
      }
      Instant parsed = parseDate(dataFormatter.formatCellValue(cell));
      if (parsed != null) {
        return parsed;
      }
    }
    return null;
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
        (header, index) ->
            values.put(header, checkedRaw(dataFormatter.formatCellValue(row.getCell(index)))));
    return Map.copyOf(values);
  }

  private Map<String, String> rawData(Map<String, Integer> headers, List<String> row) {
    Map<String, String> values = new LinkedHashMap<>();
    headers.forEach(
        (header, index) ->
            values.put(header, index < row.size() ? checkedRaw(row.get(index)) : ""));
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

  private char detectDelimiter(String content) {
    int commas = 0;
    int semicolons = 0;
    boolean quoted = false;
    for (int index = 0; index < content.length(); index++) {
      char character = content.charAt(index);
      if (character == '"') {
        if (quoted && index + 1 < content.length() && content.charAt(index + 1) == '"') {
          index++;
        } else {
          quoted = !quoted;
        }
      } else if (!quoted && (character == '\n' || character == '\r')) {
        break;
      } else if (!quoted && character == ',') {
        commas++;
      } else if (!quoted && character == ';') {
        semicolons++;
      }
    }
    return semicolons > commas ? ';' : ',';
  }

  private List<List<String>> parseCsvRows(String content, char delimiter) {
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
      } else if (character == delimiter && !quoted) {
        row.add(field.toString());
        field.setLength(0);
      } else if ((character == '\n' || character == '\r') && !quoted) {
        if (character == '\r'
            && index + 1 < content.length()
            && content.charAt(index + 1) == '\n') {
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
        if (field.length() > MAX_CELL_CHARACTERS) {
          throw new IllegalArgumentException("Import cell exceeds 10000 characters");
        }
      }
    }
    if (quoted) {
      throw new IllegalArgumentException("The CSV file contains an unclosed quoted field");
    }
    row.add(field.toString());
    if (row.stream().anyMatch(value -> !value.isBlank())) {
      rows.add(List.copyOf(row));
    }
    return List.copyOf(rows);
  }

  private String checked(String value) {
    if (value != null && value.length() > MAX_CELL_CHARACTERS) {
      throw new IllegalArgumentException("Import cell exceeds 10000 characters");
    }
    return normalizationService.trimToNull(value);
  }

  private String checkedRaw(String value) {
    if (value != null && value.length() > MAX_CELL_CHARACTERS) {
      throw new IllegalArgumentException("Import cell exceeds 10000 characters");
    }
    return value == null ? "" : value;
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
