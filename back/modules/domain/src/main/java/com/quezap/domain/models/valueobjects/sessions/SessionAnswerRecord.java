package com.quezap.domain.models.valueobjects.sessions;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record SessionAnswerRecord(
    UUID id,
    String participantName,
    int slideIndex,
    int answerIndex,
    @Nullable String textAnswer,
    int pointsAwarded,
    boolean isCorrect,
    Instant answeredAt) {}
