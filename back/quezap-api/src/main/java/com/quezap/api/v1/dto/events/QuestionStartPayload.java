package com.quezap.api.v1.dto.events;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record QuestionStartPayload(
    int slideIndex,
    int totalSlides,
    int timerSeconds,
    int points,
    SafeQuestionDto question,
    @Nullable Instant deadline,
    int answeredCount,
    int remainingSeconds) {

  public QuestionStartPayload(
      int slideIndex, int totalSlides, int timerSeconds, int points, SafeQuestionDto question) {
    this(slideIndex, totalSlides, timerSeconds, points, question, null, 0, timerSeconds);
  }

  public QuestionStartPayload(
      int slideIndex,
      int totalSlides,
      int timerSeconds,
      int points,
      SafeQuestionDto question,
      @Nullable Instant deadline) {
    this(slideIndex, totalSlides, timerSeconds, points, question, deadline, 0, timerSeconds);
  }
}
