package com.quezap.domain.models.valueobjects;

import java.util.UUID;

import com.quezap.domain.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QuestionSlideTest {
  @Test
  void canInstantiateQuestionSlide() {
    // GIVEN
    var timer = 30;
    var points = 10;
    var questionId = new QuestionId(UUID.fromString("017f5a80-7e6d-7e6d-0000-000000000000"));

    // WHEN
    new QuestionSlide(timer, points, questionId);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateQuestionSlideWithNegativeTimer() {
    // GIVEN
    var timer = -1;
    var points = 10;
    var questionId = new QuestionId(UUID.fromString("017f5a80-7e6d-7e6d-0000-000000000000"));

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new QuestionSlide(timer, points, questionId);
        });
  }

  @Test
  void cannotInstantiateQuestionSlideWithNegativePoints() {
    // GIVEN
    var timer = 30;
    var points = -5;
    var questionId = new QuestionId(UUID.fromString("017f5a80-7e6d-7e6d-0000-000000000000"));

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new QuestionSlide(timer, points, questionId);
        });
  }
}
