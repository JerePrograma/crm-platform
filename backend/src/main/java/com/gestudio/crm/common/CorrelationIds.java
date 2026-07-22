package com.gestudio.crm.common;

import java.util.UUID;

public final class CorrelationIds {

  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private CorrelationIds() {}

  public static String currentOrCreate() {
    String current = CURRENT.get();
    return current == null ? UUID.randomUUID().toString() : current;
  }

  static void set(String correlationId) {
    CURRENT.set(correlationId);
  }

  static void clear() {
    CURRENT.remove();
  }
}
