package com.quezap.api.v1.dto.request.quezaps;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateQuezapRequest(
    @NotBlank(message = "Title cannot be blank") @Size(max = 100, message = "Title cannot exceed 100 characters") String title,
    @Nullable String description,
    @Nullable Boolean isPublic,
    @NotNull(message = "Questions list cannot be null") @Size(min = 1, max = 60, message = "Quezap must contain between 1 and 60 questions") List<@Valid QuezapSlideRequest> slides) {}
