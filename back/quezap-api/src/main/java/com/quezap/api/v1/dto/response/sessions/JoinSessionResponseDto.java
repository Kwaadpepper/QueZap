package com.quezap.api.v1.dto.response.sessions;

import java.util.UUID;

public record JoinSessionResponseDto(
    UUID sessionId, String participationToken, String participantName) {}
