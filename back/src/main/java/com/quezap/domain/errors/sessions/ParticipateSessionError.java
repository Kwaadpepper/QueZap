package com.quezap.domain.errors.sessions;

import com.quezap.lib.ddd.exceptions.DomainErrorCode;

public enum ParticipateSessionError implements DomainErrorCode {
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
