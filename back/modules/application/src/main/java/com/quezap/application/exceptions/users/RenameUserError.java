package com.quezap.application.exceptions.users;

import com.quezap.application.exceptions.ApplicationErrorCode;

public enum RenameUserError implements ApplicationErrorCode {
  NO_SUCH_USER(1501, "No such user"),
  USER_NAME_ALREADY_TAKEN(1502, "User name is already taken");

  private final int code;
  private final String message;

  private RenameUserError(int code, String message) {
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
