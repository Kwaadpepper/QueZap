package com.quezap.api.v1.dto.response.sessions;

import java.util.List;
import java.util.UUID;

public record LeaderboardResponseDto(
    UUID sessionId, Integer slideIndex, List<LeaderboardEntryDto> leaderboard) {}
