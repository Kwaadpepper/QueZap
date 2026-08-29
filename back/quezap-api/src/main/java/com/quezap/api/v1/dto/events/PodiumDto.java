package com.quezap.api.v1.dto.events;

import org.jspecify.annotations.Nullable;

public record PodiumDto(String first, @Nullable String second, @Nullable String third) {}
