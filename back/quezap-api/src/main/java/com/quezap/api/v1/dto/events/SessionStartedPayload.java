package com.quezap.api.v1.dto.events;

import java.time.Instant;

public record SessionStartedPayload(
    Instant startedAt, int totalSlides, int firstSlideCountdownSeconds) {}
