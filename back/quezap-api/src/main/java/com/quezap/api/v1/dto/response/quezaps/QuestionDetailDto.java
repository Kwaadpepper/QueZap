package com.quezap.api.v1.dto.response.quezaps;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record QuestionDetailDto(
    UUID id,
    String type,
    String label,
    @Nullable String pictureUrl,
    ThemeDetailDto theme,
    List<AnswerDetailDto> answers) {}
