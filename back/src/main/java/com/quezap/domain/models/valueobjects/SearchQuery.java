package com.quezap.domain.models.valueobjects;

import com.quezap.lib.utils.Domain;

public record SearchQuery(String value) {
  public SearchQuery {
    Domain.checkDomain(() -> !value.isBlank(), "Search query cannot be blank");
    Domain.checkDomain(() -> value.length() <= 120, "Search query cannot exceed 120 characters");
  }
}
