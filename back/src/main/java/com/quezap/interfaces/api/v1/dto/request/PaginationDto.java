package com.quezap.interfaces.api.v1.dto.request;

import com.quezap.interfaces.api.v1.validation.PaginationRange;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.jspecify.annotations.Nullable;

@PaginationRange
public record PaginationDto(
    @Nullable @Positive Long page,
    @Nullable @Positive Long perPage,
    @Nullable @PositiveOrZero Long from,
    @Nullable @Positive Long to) {}
