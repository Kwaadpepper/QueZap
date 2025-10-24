package com.quezap.application.api.v1.dto.request;

import com.quezap.application.api.v1.validation.PaginationRange;

import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;

@PaginationRange
public record PaginationDto(
    @Nullable @Positive Long page,
    @Nullable @Positive Long perPage,
    @Nullable @Positive Long from,
    @Nullable @Positive Long to) {}
