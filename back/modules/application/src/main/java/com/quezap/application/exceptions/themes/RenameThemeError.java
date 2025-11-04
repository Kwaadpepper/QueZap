package com.quezap.application.exceptions.themes;

import com.quezap.application.exceptions.ApplicationErrorCode;

public enum RenameThemeError implements ApplicationErrorCode {
  THEME_DOES_NOT_EXISTS(2801, "No theme exists with the provided identifier"),
  THEME_NAME_ALREADY_EXISTS(2802, "A theme with the provided name already exists");

  private final int code;
  private final String message;

  private RenameThemeError(int code, String message) {
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
