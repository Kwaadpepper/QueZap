package com.quezap.api.v1.dto.request.quezaps;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record InlineQuestionRequest(
    @Nullable UUID id,
    @NotBlank(message = "Question label cannot be blank") String label,
    @Nullable String type,
    @Nullable String pictureFilename,
    @Nullable UUID themeId,
    @Nullable String themeName,
    @Nullable List<@Valid InlineAnswerRequest> answers) {}
