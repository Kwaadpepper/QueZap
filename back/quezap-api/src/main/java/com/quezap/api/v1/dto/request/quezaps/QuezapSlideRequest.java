package com.quezap.api.v1.dto.request.quezaps;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

public record QuezapSlideRequest(
    @Nullable UUID questionId,
    @Nullable @Min(value = 5, message = MIN) @Max(value = 300, message = MAX) Integer timer,
    @Nullable @Min(value = 5, message = MIN) @Max(value = 300, message = MAX) Integer timerSeconds,
    @Nullable @Min(value = 0, message = "Points must be positive") Integer points,
    @Nullable @Valid InlineQuestionRequest question) {
  static final String MIN = "Timer must be at least 5s";
  static final String MAX = "Timer cannot exceed 300s";

  public QuezapSlideRequest(UUID questionId, Integer timer, Integer points) {
    this(questionId, timer, timer, points, null);
  }

  public int getEffectiveTimer() {
    if (timer != null && timer > 0) {
      return timer;
    }
    if (timerSeconds != null && timerSeconds > 0) {
      return timerSeconds;
    }
    return 30;
  }
}
