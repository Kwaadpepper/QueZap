package com.quezap.api.v1.dto.request.quezaps;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record QuezapSlideRequest(
    @NotNull(message = "Question ID is required") UUID questionId,
    @Nullable @Min(value = 5, message = "Timer must be at least 5s") @Max(value = 300, message = "Timer cannot exceed 300s") Integer timer,
    @Nullable @Min(value = 0, message = "Points must be positive") Integer points) {}
