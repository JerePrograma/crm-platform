package com.gestudio.crm.common;

public final class CsvSafety {

  private CsvSafety() {}

  public static String cell(Object value) {
    String safe = value == null ? "" : value.toString();
    if (!safe.isEmpty() && ("=+-@\t\r".indexOf(safe.charAt(0)) >= 0)) {
      safe = "'" + safe;
    }
    return '"' + safe.replace("\"", "\"\"") + '"';
  }
}
