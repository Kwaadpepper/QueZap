package com.quezap.domain.models.valueobjects;

import com.quezap.lib.utils.Domain;

public record SessionNumber(long value) {
  public SessionNumber {
    Domain.checkDomain(() -> value > 0L, "Session raw code must be a positive long");
    Domain.checkDomain(
        () -> String.valueOf(value).length() <= 1000000000,
        "Session raw code must not exceed 1,000,000,000");
  }
}
