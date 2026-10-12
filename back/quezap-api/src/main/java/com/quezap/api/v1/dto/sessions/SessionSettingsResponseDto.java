package com.quezap.api.v1.dto.sessions;

public record SessionSettingsResponseDto(
    String progressionMode, int explanationDurationSeconds, boolean mobileCorrectionEnabled) {}
