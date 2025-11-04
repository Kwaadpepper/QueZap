package com.quezap.application.exceptions.sessions;

import com.quezap.application.exceptions.ApplicationErrorCode;

public enum StartSessionError implements ApplicationErrorCode {
  SESSION_ALREADY_STARTED(2101, "Session has already started"),
  SESSION_ENDED(2102, "Session has already been ended"),
  NO_SUCH_SESSION(2103, "Session not found"),
  NOT_ENOUGH_PARTICIPANTS(2104, "Not enough participants to start the session"),
  NOT_ENOUGH_QUESTIONS_TO_START(2105, "Not enough questions to start the session");

  private final int code;
  private final String message;

  private StartSessionError(int code, String message) {
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
