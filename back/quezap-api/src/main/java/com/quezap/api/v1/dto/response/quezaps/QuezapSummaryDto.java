package com.quezap.api.v1.dto.response.quezaps;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record QuezapSummaryDto(
    UUID id,
    String title,
    @Nullable String description,
    Boolean isPublic,
    UUID authorId,
    int totalQuestions,
    Instant createdAt) {}
