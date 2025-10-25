package com.quezap.domain.errors.questions;

import com.quezap.lib.ddd.exceptions.DomainErrorCode;

public enum AddQuestionError implements DomainErrorCode {
  THEME_DOES_NOT_EXISTS(2901, "The specified theme does not exist"),
  INVALID_QUESTION_DATA(2902, "The provided question data is invalid");

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
