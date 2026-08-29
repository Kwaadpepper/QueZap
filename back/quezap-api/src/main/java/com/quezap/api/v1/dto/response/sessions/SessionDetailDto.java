package com.quezap.api.v1.dto.response.sessions;

import java.util.UUID;

public record SessionDetailDto(
    UUID id,
    Long number,
    String code,
    String name,
    String status,
    Integer currentSlideIndex,
    Integer totalSlides,
    Integer participantsCount,
    UUID authorId) {}
