package com.quezap.domain.models.entities;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.quezap.domain.exceptions.IllegalDomainStateException;
import com.quezap.domain.models.valueobjects.QuestionId;
import com.quezap.domain.models.valueobjects.QuestionSlide;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.UserId;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SessionTest {

  @Test
  void canInstantiate() {
    // GIVEN
    var label = "Session 1";
    var sessionCode = new SessionCode("B1C3");
    Set<QuestionSlide> questionSlides =
        Set.of(
            new QuestionSlide(
                10, 1, new QuestionId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"))),
            new QuestionSlide(
                12, 2, new QuestionId(UUID.fromString("017f5a80-7e6d-7e6d-0000-000000000000"))),
            new QuestionSlide(
                8, 3, new QuestionId(UUID.fromString("017f5a80-7e6d-7e6f-0000-000000000000"))));
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.plusHours(1);

    // WHEN
    new Session(label, sessionCode, questionSlides, userId, startedAt, endedAt);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateWithBlankLabel() {
    // GIVEN
    var label = "   ";
    var sessionCode = new SessionCode("B1C3");
    Set<QuestionSlide> questionSlides =
        Set.of(
            new QuestionSlide(
                10, 1, new QuestionId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"))));
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.plusHours(1);

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Session(label, sessionCode, questionSlides, userId, startedAt, endedAt);
        });
  }

  @Test
  void cannotInstatiateWithLabelTooLong() {
    // GIVEN
    var label = "S".repeat(256);
    var sessionCode = new SessionCode("B1C3");
    Set<QuestionSlide> questionSlides =
        Set.of(
            new QuestionSlide(
                10, 1, new QuestionId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"))));
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.plusHours(1);

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Session(label, sessionCode, questionSlides, userId, startedAt, endedAt);
        });
  }

  @Test
  void cannotInstantiateWithEmptyQuestionSlides() {
    // GIVEN
    var label = "Session 1";
    var sessionCode = new SessionCode("B1C3");
    Set<QuestionSlide> questionSlides = Set.of();
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.plusHours(1);

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Session(label, sessionCode, questionSlides, userId, startedAt, endedAt);
        });
  }

  @Test
  void cannotInstantiateWithTooManyQuestionSlides() {
    // GIVEN
    var label = "Session 1";
    var sessionCode = new SessionCode("B1C3");
    Set<QuestionSlide> questionSlides = new HashSet<>();
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.plusHours(1);

    for (int i = 1; i <= 61; i++) {
      questionSlides.add(
          new QuestionSlide(
              10 * i,
              i,
              new QuestionId(
                  UUID.fromString(
                      "017f5a80-7e6d-7e6e-0000-0000000000" + String.format("%02d", i)))));
    }

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Session(label, sessionCode, questionSlides, userId, startedAt, endedAt);
        });
  }

  @Test
  void cannotInstantiateWithInvalidDates() {
    // GIVEN
    var label = "Session 1";
    var sessionCode = new SessionCode("B1C3");
    Set<QuestionSlide> questionSlides =
        Set.of(
            new QuestionSlide(
                10, 1, new QuestionId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"))));
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.minusHours(1);

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Session(label, sessionCode, questionSlides, userId, startedAt, endedAt);
        });
  }
}
