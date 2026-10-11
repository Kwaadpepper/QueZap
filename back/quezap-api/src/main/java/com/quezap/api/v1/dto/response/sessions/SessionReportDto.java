package com.quezap.api.v1.dto.response.sessions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record SessionReportDto(
    UUID sessionId,
    Long sessionNumber,
    String sessionCode,
    String sessionName,
    @Nullable Instant startedAt,
    @Nullable Instant endedAt,
    int totalParticipants,
    int totalQuestions,
    double averageScore,
    double averageAccuracyRate,
    List<SessionParticipantReportDto> participants,
    List<QuestionReportDto> questions) {}
