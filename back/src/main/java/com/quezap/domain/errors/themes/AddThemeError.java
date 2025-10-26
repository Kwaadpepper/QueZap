package com.quezap.domain.errors.themes;

import com.quezap.lib.ddd.exceptions.DomainErrorCode;

public enum AddThemeError implements DomainErrorCode {
  THEME_ALREADY_EXISTS(3001, "A theme with the same name already exists");

  private final int code;
  private final String message;

  private AddThemeError(int code, String message) {
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
