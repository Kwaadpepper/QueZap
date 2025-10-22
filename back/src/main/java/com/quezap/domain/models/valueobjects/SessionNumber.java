package com.quezap.domain.models.valueobjects;

import com.quezap.lib.utils.Domain;

public record SessionNumber(Integer value) {
  public SessionNumber {
    Domain.checkDomain(() -> value > 0, "Session raw code must be a positive integer");
    Domain.checkDomain(
        () -> String.valueOf(value.intValue()).length() <= 1000000,
        "Session raw code must not exceed 1,000,000");
  }
}
