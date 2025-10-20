package com.quezap.domain.models.valueobjects.participations;

import com.quezap.lib.utils.Domain;

public record Participant(ParticipantName name, Integer score, ParticipationToken token) {
  public Participant {
    Domain.checkDomain(() -> score >= 0, "A participant score must be greater than or equal to 0");
  }
}
