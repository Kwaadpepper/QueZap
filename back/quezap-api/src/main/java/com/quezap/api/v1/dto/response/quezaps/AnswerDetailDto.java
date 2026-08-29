package com.quezap.api.v1.dto.response.quezaps;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record AnswerDetailDto(
    UUID id, @Nullable String label, Boolean isCorrect, @Nullable String pictureUrl) {}
