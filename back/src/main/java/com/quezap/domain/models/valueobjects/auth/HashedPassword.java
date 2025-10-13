package com.quezap.domain.models.valueobjects.auth;

import com.quezap.lib.utils.Domain;

public record HashedPassword(String value) {
  public HashedPassword {
    Domain.checkDomain(() -> !value.isBlank(), "Hashed password cannot be blank");
    Domain.checkDomain(
        () -> containsHashedValue(value),
        "Hashed password value may look like $...$...$... and be a hash");
  }

  @Override
  public String toString() {
    return "[PROTECTED]";
  }

  private boolean containsHashedValue(String value) {
    return value.matches("^\\$.+\\$.+\\$.+\\$?");
  }
}
