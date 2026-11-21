package com.quezap.application.exceptions.users;

import com.quezap.application.exceptions.ApplicationErrorCode;

public enum AuthenticateUserError implements ApplicationErrorCode {
  INVALID_CREDENTIALS(1401, "Invalid credentials"),
  USER_NOT_FOUND(1402, "User account not found"),
  ACCOUNT_NOT_ACTIVATED(1403, "User account is not activated");

  private final int code;
  private final String message;

  private AuthenticateUserError(int code, String message) {
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
