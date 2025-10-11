package com.quezap.domain.models.valueobjects;

import com.quezap.lib.utils.Domain;

public record HashedIdentifier(String value) {
  public HashedIdentifier {
    Domain.checkDomain(() -> !value.isBlank(), "Hash identifier cannot be blank");
  }

  @Override
  public String toString() {
    return "[PROTECTED]";
  }
}
