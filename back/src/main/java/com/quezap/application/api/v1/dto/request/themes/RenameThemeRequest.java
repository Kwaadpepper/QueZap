package com.quezap.application.api.v1.dto.request.themes;

import com.quezap.domain.models.valueobjects.ThemeName;

import jakarta.validation.constraints.NotNull;

public record RenameThemeRequest(@NotNull ThemeName name) {}
