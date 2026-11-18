package com.quezap.application.exceptions.users;

import com.quezap.application.exceptions.ApplicationErrorCode;

public enum FindUserError implements ApplicationErrorCode {
  NO_SUCH_USER(1301, "No such user");

  private final int code;
  private final String message;

  private FindUserError(int code, String message) {
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
