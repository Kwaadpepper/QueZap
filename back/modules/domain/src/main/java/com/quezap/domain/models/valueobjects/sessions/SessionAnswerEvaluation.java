package com.quezap.domain.models.valueobjects.sessions;

public record SessionAnswerEvaluation(boolean isCorrect, int pointsAwarded, long elapsedMillis) {}
