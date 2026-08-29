package com.quezap.api.v1.dto.request.quezaps;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QuezapSlideRequest(
    @NotNull(message = "QuestionId cannot be null") UUID questionId,
    @Min(value = 5, message = "Timer must be at least 5 seconds") @Max(value = 300, message = "Timer cannot exceed 300 seconds") Integer timer,
    @Min(value = 1, message = "Points must be greater than 0") Integer points) {}
