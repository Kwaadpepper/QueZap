package com.quezap.api.v1.dto.response.sessions;

public record QuestionAnswerStatDto(
    int answerIndex, String label, boolean isCorrect, int count, double percentage) {}
