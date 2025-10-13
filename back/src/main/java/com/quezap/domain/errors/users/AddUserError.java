package com.quezap.domain.errors.users;

import com.quezap.lib.ddd.exceptions.DomainErrorCode;

public enum AddUserError implements DomainErrorCode {
  IDENTIFIER_ALREADY_TAKEN(1001, "Identifier is already taken"),
  USER_NAME_ALREADY_TAKEN(1002, "User name is already taken");

  private final int code;
  private final String message;

  private AddUserError(int code, String message) {
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
