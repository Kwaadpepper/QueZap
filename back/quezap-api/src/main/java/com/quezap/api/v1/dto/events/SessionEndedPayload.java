package com.quezap.api.v1.dto.events;

import java.time.Instant;
import java.util.List;

public record SessionEndedPayload(
    Instant endedAt, List<FinalLeaderboardEntryDto> finalLeaderboard, PodiumDto podium) {}
