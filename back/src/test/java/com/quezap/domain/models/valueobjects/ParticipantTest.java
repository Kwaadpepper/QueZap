package com.quezap.domain.models.valueobjects;

import com.quezap.domain.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParticipantTest {

  @Test
  void canInstantiateParticipant() {
    // GIVEN
    var name = "John Doe";
    var score = 10;

    // WHEN
    new Participant(name, score);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateParticipantWithBlankName() {
    // GIVEN
    var name = "   ";
    var score = 5;

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Participant(name, score);
        });
  }

  @Test
  void cannotInstantiateParticipantWithNegativeScore() {
    // GIVEN
    var name = "Jane Doe";
    var score = -1;

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Participant(name, score);
        });
  }
}
