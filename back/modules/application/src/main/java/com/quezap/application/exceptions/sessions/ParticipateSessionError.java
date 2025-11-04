package com.quezap.application.exceptions.sessions;

import com.quezap.application.exceptions.ApplicationErrorCode;

public enum ParticipateSessionError implements ApplicationErrorCode {
  INVALID_CODE(2001, "The session code is invalid"),
  NAME_ALREADY_TAKEN(2002, "User name is already taken"),
  NAME_REFUSED(2003, "User name is not compliant with our policies");

  private final int code;
  private final String message;

  private ParticipateSessionError(int code, String message) {
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
