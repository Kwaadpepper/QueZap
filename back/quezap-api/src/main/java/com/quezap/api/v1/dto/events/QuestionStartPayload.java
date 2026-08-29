package com.quezap.api.v1.dto.events;

public record QuestionStartPayload(
    int slideIndex, int totalSlides, int timerSeconds, int points, SafeQuestionDto question) {}
