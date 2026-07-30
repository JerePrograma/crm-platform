package com.gestudio.crm.gmail;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public final class GmailProblemException extends ErrorResponseException {

  private final String code;

  public GmailProblemException(HttpStatus status, String code, String detail) {
    super(status, problem(status, code, detail), null);
    this.code = code;
  }

  public String code() {
    return code;
  }

  private static ProblemDetail problem(HttpStatus status, String code, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle("Gmail integration error");
    problem.setProperty("code", code);
    return problem;
  }
}
