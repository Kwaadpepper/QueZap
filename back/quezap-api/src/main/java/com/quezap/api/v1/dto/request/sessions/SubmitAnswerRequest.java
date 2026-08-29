package com.quezap.api.v1.dto.request.sessions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitAnswerRequest(
    @NotNull(message = "Slide index cannot be null") @Min(value = 0, message = "Slide index must be non-negative") Integer slideIndex,
    @NotNull(message = "Answer index cannot be null") @Min(value = 0, message = "Answer index must be non-negative") Integer answerIndex) {}
