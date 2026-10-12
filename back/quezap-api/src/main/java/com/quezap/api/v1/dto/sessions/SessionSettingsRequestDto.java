package com.quezap.api.v1.dto.sessions;

import org.jspecify.annotations.Nullable;

public record SessionSettingsRequestDto(
    @Nullable String progressionMode,
    @Nullable Integer explanationDurationSeconds,
    @Nullable Boolean mobileCorrectionEnabled) {}
