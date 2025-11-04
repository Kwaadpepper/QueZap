package com.quezap.application.exceptions.sessions;

import com.quezap.application.exceptions.ApplicationErrorCode;

public enum RemoveQuestionError implements ApplicationErrorCode {
  NO_SUCH_QUESTION(2501, "The question does not exist"),
  NO_SUCH_SESSION(2502, "The session does not exist"),
  SESSION_IS_RUNNING(2504, "Cannot remove question to a session that is running"),
  SESSION_IS_ENDED(2505, "Cannot remove question to a session that has ended");

  private final int code;
  private final String message;

  private RemoveQuestionError(int code, String message) {
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
