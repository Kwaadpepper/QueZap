package com.quezap.domain.models.valueobjects.auth;

import com.quezap.lib.utils.Domain;

public record RawPassword(String value) {
  private static final String MIXED_CASE_REGEX =
      "(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9!-\\/:-@\\[-`\\{-~]{2,}";
  private static final String CONTAINS_NUMBER_REGEX = ".*\\d+.*";
  private static final String CONTAINS_SPECIAL_CHAR_REGEX = ".*[!-\\/:-@\\[-`\\{-~]+.*";

  public RawPassword {
    Domain.checkDomain(() -> !value.isBlank(), "Raw password cannot be blank");
    Domain.checkDomain(
        () -> value.length() >= 8, "Raw password must be at least 8 characters long");
    Domain.checkDomain(
        () -> value.length() <= 80, "Raw password must be at most 80 characters long");
    Domain.checkDomain(
        () -> value.matches(MIXED_CASE_REGEX), "Raw password must contain mixed case letters");
    Domain.checkDomain(
        () -> value.matches(CONTAINS_NUMBER_REGEX), "Raw password must contain numbers");
    Domain.checkDomain(
        () -> value.matches(CONTAINS_SPECIAL_CHAR_REGEX),
        "Raw password must contain special characters");
  }

  @Override
  public String toString() {
    return "[PROTECTED]";
  }
}
