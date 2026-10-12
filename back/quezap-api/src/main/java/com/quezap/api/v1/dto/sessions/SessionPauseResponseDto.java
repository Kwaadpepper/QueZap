package com.quezap.api.v1.dto.sessions;

import java.util.UUID;

public record SessionPauseResponseDto(UUID sessionId, boolean isPaused) {}
