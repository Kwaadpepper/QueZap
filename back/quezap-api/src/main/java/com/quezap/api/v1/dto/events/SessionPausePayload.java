package com.quezap.api.v1.dto.events;

import java.time.Instant;

public record SessionPausePayload(boolean isPaused, Instant timestamp) {}
