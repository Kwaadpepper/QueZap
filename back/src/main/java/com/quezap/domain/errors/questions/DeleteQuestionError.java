package com.quezap.domain.errors.questions;

import com.quezap.lib.ddd.exceptions.DomainErrorCode;

public enum DeleteQuestionError implements DomainErrorCode {
  QUESTION_NOT_FOUND(3101, "The specified question does not exist");

  private final int code;
  private final String message;

  private DeleteQuestionError(int code, String message) {
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
