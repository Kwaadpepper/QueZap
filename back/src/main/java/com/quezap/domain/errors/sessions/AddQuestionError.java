package com.quezap.domain.errors.sessions;

import com.quezap.lib.ddd.exceptions.DomainErrorCode;

public enum AddQuestionError implements DomainErrorCode {
  NO_SUCH_QUESTION(4001, "The question does not exist"),
  NO_SUCH_SESSION(4002, "The session does not exist"),
  MAX_QUESTIONS_REACHED(4003, "The session has reached the maximum number of questions");

  private final int code;
  private final String message;

  private AddQuestionError(int code, String message) {
    this.code = code;
    this.message = message;
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
