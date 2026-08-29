package com.quezap.api.v1.dto.events;

public record FinalLeaderboardEntryDto(
    int rank, String participantName, int totalScore, int correctCount) {}
