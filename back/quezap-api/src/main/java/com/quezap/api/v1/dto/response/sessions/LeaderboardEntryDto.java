package com.quezap.api.v1.dto.response.sessions;

public record LeaderboardEntryDto(
    int rank, String participantName, int score, int delta, int streak) {}
