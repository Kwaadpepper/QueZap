package com.quezap.domain.models.entities;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.questions.QuestionAnswer;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SessionTest {

  @Test
  void canInstantiate() {
    // GIVEN
    var label = new SessionName("Session 1");
    var sessionCode = new SessionNumber(2);
    var currentSlideIndex = 0;
    var questionSlides =
        Set.of(
            new QuestionSlide(10, 1, QuestionId.fromString("017f5a80-7e6d-7e6e-0000-000000000000")),
            new QuestionSlide(12, 2, QuestionId.fromString("017f5a80-7e6d-7e6d-0000-000000000000")),
            new QuestionSlide(8, 3, QuestionId.fromString("017f5a80-7e6d-7e6f-0000-000000000000")));
    var participants = Set.<Participant>of();
    var answers = Set.<QuestionAnswer>of();
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.plusHours(1);

    // WHEN
    new Session(
        label,
        sessionCode,
        currentSlideIndex,
        questionSlides,
        participants,
        answers,
        userId,
        startedAt,
        endedAt);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateWithEmptyQuestionSlides() {
    // GIVEN
    var label = new SessionName("Session 1");
    var sessionCode = new SessionNumber(2);
    var currentSlideIndex = 0;
    var questionSlides = Set.<QuestionSlide>of();
    var participants = Set.<Participant>of();
    var answers = Set.<QuestionAnswer>of();
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.plusHours(1);

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Session(
              label,
              sessionCode,
              currentSlideIndex,
              questionSlides,
              participants,
              answers,
              userId,
              startedAt,
              endedAt);
        });
  }

  @Test
  void cannotInstantiateWithTooManyQuestionSlides() {
    // GIVEN
    var label = new SessionName("Session 1");
    var sessionCode = new SessionNumber(2);
    var currentSlideIndex = 0;
    var questionSlides = new HashSet<QuestionSlide>();
    var participants = Set.<Participant>of();
    var answers = Set.<QuestionAnswer>of();
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.plusHours(1);

    for (int i = 1; i <= 61; i++) {
      questionSlides.add(
          new QuestionSlide(
              10 * i,
              i,
              QuestionId.fromString(
                  "017f5a80-7e6d-7e6e-0000-0000000000" + String.format("%02d", i))));
    }

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Session(
              label,
              sessionCode,
              currentSlideIndex,
              questionSlides,
              participants,
              answers,
              userId,
              startedAt,
              endedAt);
        });
  }

  @Test
  void cannotInstantiateWithInvalidDates() {
    // GIVEN
    var label = new SessionName("Session 1");
    var sessionCode = new SessionNumber(2);
    var currentSlideIndex = 0;
    var questionSlides =
        Set.of(
            new QuestionSlide(
                10, 1, QuestionId.fromString("017f5a80-7e6d-7e6e-0000-000000000000")));
    var participants = Set.<Participant>of();
    var answers = Set.<QuestionAnswer>of();
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.minusHours(1);

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Session(
              label,
              sessionCode,
              currentSlideIndex,
              questionSlides,
              participants,
              answers,
              userId,
              startedAt,
              endedAt);
        });
  }

  @Test
  void cannotInstanciateWithInvalidSlideIndex() {
    // GIVEN
    var label = new SessionName("Session 1");
    var sessionCode = new SessionNumber(2);
    var currentSlideIndex = 5;
    var questionSlides =
        Set.of(
            new QuestionSlide(10, 1, QuestionId.fromString("017f5a80-7e6d-7e6e-0000-000000000000")),
            new QuestionSlide(
                12, 2, QuestionId.fromString("017f5a80-7e6d-7e6d-0000-000000000000")));
    var participants = Set.<Participant>of();
    var answers = Set.<QuestionAnswer>of();
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var startedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var endedAt = startedAt.plusHours(1);

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Session(
              label,
              sessionCode,
              currentSlideIndex,
              questionSlides,
              participants,
              answers,
              userId,
              startedAt,
              endedAt);
        });
  }
}
