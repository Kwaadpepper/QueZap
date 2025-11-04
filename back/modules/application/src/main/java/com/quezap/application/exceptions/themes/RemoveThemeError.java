package com.quezap.application.exceptions.themes;

import com.quezap.application.exceptions.ApplicationErrorCode;

public enum RemoveThemeError implements ApplicationErrorCode {
  THEME_DOES_NOT_EXISTS(2701, "No theme exists with the provided identifier"),
  THEME_HAS_QUESTIONS(2702, "The theme has questions associated and cannot be removed");

  private final int code;
  private final String message;

  private RemoveThemeError(int code, String message) {
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
