package com.quezap.domain.models.valueobjects;

import com.quezap.lib.utils.Domain;

public record Participant(String name, Integer score) {
  public Participant {
    Domain.checkDomain(() -> !name.isBlank(), "A participant name must not be blank");
    Domain.checkDomain(
        () -> name.length() <= 65, "A participant name must not exceed 65 characters");
    Domain.checkDomain(() -> score >= 0, "A participant score must be greater than or equal to 0");
  }
}
