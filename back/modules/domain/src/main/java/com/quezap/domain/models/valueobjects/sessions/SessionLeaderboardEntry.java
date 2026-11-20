package com.quezap.domain.models.valueobjects.sessions;

public record SessionLeaderboardEntry(
    int rank, String participantName, int score, int delta, int streak, int correctCount) {}
