package com.quezap.domain.models.valueobjects.auth;

import com.quezap.lib.utils.Domain;

public record PasswordCandidate(String value) {
  public PasswordCandidate {
    Domain.checkDomain(() -> !value.isBlank(), "password cannot be blank");
  }

  @Override
  public String toString() {
    return "[PROTECTED]";
  }
}
