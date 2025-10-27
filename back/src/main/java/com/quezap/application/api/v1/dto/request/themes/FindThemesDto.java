package com.quezap.application.api.v1.dto.request.themes;

import com.quezap.domain.models.valueobjects.SearchQuery;

import org.jspecify.annotations.Nullable;

public record FindThemesDto(@Nullable @jakarta.annotation.Nullable SearchQuery search) {}
