package com.quezap.domain.errors.sessions;

import com.quezap.lib.ddd.exceptions.DomainErrorCode;

public enum AnswerSessionError implements DomainErrorCode {
  NO_SUCH_SESSION(2601, "The session does not exist"),
  INVALID_PARTICIPATION_TOKEN(2602, "The participation token is invalid"),
  SESSION_NOT_STARTED(2603, "The session has not started yet"),
  SESSION_ALREADY_ENDED(2604, "The session has already ended"),
  INVALID_SLIDE_INDEX(2605, "The slide index is invalid"),
  INVALID_ANSWER_INDEX(2606, "The answer index is invalid");

  private final int code;
  private final String message;

  private AnswerSessionError(int code, String message) {
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
