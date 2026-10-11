package com.quezap.api.v1.dto.request.quezaps;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record InlineAnswerRequest(
    @Nullable UUID id,
    @NotBlank(message = "Answer label cannot be blank") String label,
    @Nullable Boolean isCorrect,
    @Nullable String pictureFilename,
    @Nullable Integer answerOrder) {}
