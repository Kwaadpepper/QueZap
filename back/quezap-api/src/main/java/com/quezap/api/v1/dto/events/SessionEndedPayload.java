package com.quezap.api.v1.dto.events;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

public record SessionEndedPayload(
    Instant endedAt,
    List<FinalLeaderboardEntryDto> finalLeaderboard,
    PodiumDto podium,
    @Nullable SessionGroupMetricsDto metrics,
    @Nullable List<FinalLeaderboardEntryDto> top5) {}
