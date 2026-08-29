package com.quezap.api.v1.dto.events;

import org.jspecify.annotations.Nullable;

public record SafeAnswerDto(int index, @Nullable String label, @Nullable String pictureUrl) {}
