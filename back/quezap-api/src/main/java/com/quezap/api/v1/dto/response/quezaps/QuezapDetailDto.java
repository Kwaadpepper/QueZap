package com.quezap.api.v1.dto.response.quezaps;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.quezap.api.v1.dto.response.users.UserSummaryDto;

import org.jspecify.annotations.Nullable;

public record QuezapDetailDto(
    UUID id,
    String title,
    @Nullable String description,
    Boolean isPublic,
    UserSummaryDto author,
    List<QuezapSlideDetailDto> slides,
    Instant createdAt,
    Instant updatedAt) {}
