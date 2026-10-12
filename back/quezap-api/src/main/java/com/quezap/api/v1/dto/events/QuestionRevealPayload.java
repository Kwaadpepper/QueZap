package com.quezap.api.v1.dto.events;

import java.util.List;

import org.jspecify.annotations.Nullable;

public record QuestionRevealPayload(
    int slideIndex,
    List<Integer> correctAnswerIndices,
    @Nullable String explanation,
    List<AnswerStatDto> answersStats,
    @Nullable Boolean mobileCorrectionAllowed) {}
