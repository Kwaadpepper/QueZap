package com.quezap.domain.models.valueobjects;

import com.quezap.lib.utils.Domain;

public record SessionName(String value) {
  public SessionName {
    Domain.checkDomain(() -> !value.isBlank(), "Session name cannot be blank");
    Domain.checkDomain(
        () -> value.trim().length() >= 6, "Session name cannot be less than 6 characters");
    Domain.checkDomain(() -> value.length() <= 120, "Session name cannot exceed 120 characters");
  }
}
