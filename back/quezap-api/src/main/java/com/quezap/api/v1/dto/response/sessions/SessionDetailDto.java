package com.quezap.api.v1.dto.response.sessions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.quezap.api.v1.dto.events.ParticipantDto;

import org.jspecify.annotations.Nullable;

public record SessionDetailDto(
    UUID id,
    Long number,
    String code,
    String name,
    String status,
    Integer currentSlideIndex,
    Integer totalSlides,
    Integer participantsCount,
    List<ParticipantDto> participants,
    UUID authorId,
    @Nullable Instant startedAt,
    @Nullable Instant endedAt,
    String progressionMode,
    boolean isPaused,
    int explanationDurationSeconds,
    boolean mobileCorrectionEnabled) {}
