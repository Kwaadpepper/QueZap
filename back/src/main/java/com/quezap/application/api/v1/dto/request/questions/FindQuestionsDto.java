package com.quezap.application.api.v1.dto.request.questions;

import java.util.Set;

import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;

import org.jspecify.annotations.Nullable;

public record FindQuestionsDto(
    @Nullable @jakarta.annotation.Nullable SearchQuery search,
    @Nullable @jakarta.annotation.Nullable Set<ThemeId> themes) {}
