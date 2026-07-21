package com.gestudio.crm.common;

public class OptimisticConflictException extends RuntimeException {
  public OptimisticConflictException(String message) {
    super(message);
  }
}
