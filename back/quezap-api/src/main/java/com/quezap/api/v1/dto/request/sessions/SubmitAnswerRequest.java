package com.quezap.api.v1.dto.request.sessions;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitAnswerRequest(
    @NotNull(message = "Slide index is required") @Min(value = 1, message = "Slide index must be at least 1") Integer slideIndex,
    @NotNull(message = "Answer index is required") @Min(value = 0, message = "Answer index must be positive") @Max(value = 3, message = "Answer index cannot exceed 3") Integer answerIndex) {}
