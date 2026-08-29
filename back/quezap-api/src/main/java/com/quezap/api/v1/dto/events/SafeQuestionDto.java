package com.quezap.api.v1.dto.events;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record SafeQuestionDto(
    UUID id, String type, String label, @Nullable String pictureUrl, List<SafeAnswerDto> answers) {}
