package com.quezap.application.exceptions.sessions;

import com.quezap.application.exceptions.ApplicationErrorCode;

public enum AddQuestionError implements ApplicationErrorCode {
  NO_SUCH_QUESTION(2401, "The question does not exist"),
  NO_SUCH_SESSION(2402, "The session does not exist"),
  MAX_QUESTIONS_REACHED(2403, "The session has reached the maximum number of questions"),
  SESSION_IS_RUNNING(2404, "Cannot add question to a session that is running"),
  SESSION_IS_ENDED(2405, "Cannot add question to a session that has ended");

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
