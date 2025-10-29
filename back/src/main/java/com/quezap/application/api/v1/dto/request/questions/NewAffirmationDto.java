package com.quezap.application.api.v1.dto.request.questions;

import org.springframework.web.multipart.MultipartFile;

import com.quezap.domain.models.valueobjects.identifiers.ThemeId;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record NewAffirmationDto(
    @NotBlank @Size(max = 255) String question,
    boolean isTrue,
    @Nullable MultipartFile picture,
    @Nonnull ThemeId themeId) {}
