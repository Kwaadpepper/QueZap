package com.quezap.api.v1.dto.response.sessions;

public record SessionParticipantReportDto(
    int rank,
    String participantName,
    int totalScore,
    int correctCount,
    int totalQuestions,
    double accuracyRate) {}
