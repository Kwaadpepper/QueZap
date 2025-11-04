package com.quezap.domain.models.valueobjects.participations;

import com.quezap.lib.utils.Domain;

public record ParticipantName(String value) {
  public ParticipantName {
    Domain.checkDomain(() -> !value.isBlank(), "Participant name cannot be blank");
    Domain.checkDomain(
        () -> value.length() >= 3, "Participant name must be at least 3 characters long");
    Domain.checkDomain(() -> value.length() <= 18, "Participant name cannot exceed 18 characters");
  }
}
