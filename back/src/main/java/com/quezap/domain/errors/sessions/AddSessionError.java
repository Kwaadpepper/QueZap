package com.quezap.domain.errors.sessions;

import com.quezap.lib.ddd.exceptions.DomainErrorCode;

public enum AddSessionError implements DomainErrorCode {
  NO_SUCH_USER(2201, "The user does not exist");

  private final int code;
  private final String message;

  private AddSessionError(int code, String message) {
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
