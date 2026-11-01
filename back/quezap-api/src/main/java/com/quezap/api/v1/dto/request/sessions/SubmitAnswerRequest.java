package com.quezap.api.v1.dto.request.sessions;

import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

public record SubmitAnswerRequest(
    @Nullable @Min(value = 1, message = "Slide index must be at least 1") Integer slideIndex,
    @Nullable @Min(value = 0, message = "Answer index must be positive") Integer answerIndex,
    @Nullable String textAnswer) {

  public SubmitAnswerRequest(@Nullable Integer slideIndex, @Nullable Integer answerIndex) {
    this(slideIndex, answerIndex, null);
  }
}
