package com.quezap.application.api.v1.dto.request.themes;

import com.quezap.domain.models.valueobjects.ThemeName;

import jakarta.annotation.Nonnull;

public record CreateThemeRequest(@Nonnull ThemeName name) {}
