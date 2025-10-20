package com.quezap.domain.models.valueobjects;

import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParticipantTest {

  @Test
  void canInstantiateParticipant() {
    // GIVEN
    var name = new ParticipantName("John Doe");
    var score = 10;
    var token = new ParticipationToken("valid");

    // WHEN
    new Participant(name, score, token);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateParticipantWithNegativeScore() {
    // GIVEN
    var name = new ParticipantName("John Doe");
    var score = -1;
    var token = new ParticipationToken("valid");

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Participant(name, score, token);
        });
  }
}
