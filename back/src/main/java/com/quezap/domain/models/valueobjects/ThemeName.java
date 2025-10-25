package com.quezap.domain.models.valueobjects;

import com.quezap.lib.utils.Domain;

public record ThemeName(String value) {
  public ThemeName {
    Domain.checkDomain(() -> !value.isBlank(), "Theme cannot be blank");
    Domain.checkDomain(() -> value.trim().length() >= 2, "Theme cannot be less than 2 characters");
    Domain.checkDomain(() -> value.length() <= 100, "Theme cannot exceed 100 characters");
  }
}
