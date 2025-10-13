package com.quezap.domain.models.valueobjects.auth;

import com.quezap.lib.utils.Domain;

public record RawIdentifier(String value) {
  public RawIdentifier {
    Domain.checkDomain(() -> !value.isBlank(), "Raw identifier cannot be blank");
    Domain.checkDomain(
        () -> value.length() >= 4, "Raw identifier cannot be less than 4 characters long");
    Domain.checkDomain(
        () -> value.length() <= 10, "Raw identifier cannot be longer than 10 characters");
  }

  @Override
  public String toString() {
    return "[PROTECTED]";
  }
}
