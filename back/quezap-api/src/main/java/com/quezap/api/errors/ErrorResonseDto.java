package com.quezap.api.errors;

import java.time.ZonedDateTime;

import org.jspecify.annotations.Nullable;

public record ErrorResonseDto(Integer code, @Nullable String message, ZonedDateTime timestamp) {}
