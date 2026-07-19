package com.gestudio.crm.common;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleNotFound(ResourceNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
  }

  @ExceptionHandler({DuplicateResourceException.class, DataIntegrityViolationException.class})
  ProblemDetail handleConflict(Exception exception) {
    return problem(
        HttpStatus.CONFLICT,
        "Resource conflict",
        exception instanceof DuplicateResourceException
            ? exception.getMessage()
            : "The operation conflicts with an existing normalized record");
  }

  @ExceptionHandler({IllegalArgumentException.class})
  ProblemDetail handleBadRequest(IllegalArgumentException exception) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
    ProblemDetail detail =
        problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
    List<FieldViolation> violations =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
            .toList();
    detail.setProperty("errors", violations);
    return detail;
  }

  private ProblemDetail problem(HttpStatus status, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    return problem;
  }

  record FieldViolation(String field, String message) {}
}
