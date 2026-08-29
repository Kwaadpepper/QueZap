package com.quezap.api.v1.dto.events;

import java.util.List;

import com.quezap.api.v1.dto.response.sessions.LeaderboardEntryDto;

public record IntermediateLeaderboardPayload(
    int slideIndex, List<LeaderboardEntryDto> leaderboard) {}
