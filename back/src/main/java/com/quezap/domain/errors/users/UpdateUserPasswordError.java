package com.quezap.domain.errors.users;

import com.quezap.lib.ddd.exceptions.DomainErrorCode;

public enum UpdateUserPasswordError implements DomainErrorCode {
  NO_SUCH_USER(1201, "No such user");

  private final int code;
  private final String message;

  private UpdateUserPasswordError(int code, String message) {
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
