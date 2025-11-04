package com.quezap.domain.models.valueobjects;

import com.quezap.lib.utils.Domain;

public record SessionCode(String value) {
  public SessionCode {
    Domain.checkDomain(() -> !value.isBlank(), "Session code cannot be blank");
    Domain.checkDomain(() -> value.trim().length() == 6, "Session code must be 6 characters long");
    Domain.checkDomain(
        () -> value.matches("^[A-Z0-9]*$"), "Session code must be alphanumeric and uppercase");
  }
}
