package com.quezap.application.api.errors;

import java.time.ZonedDateTime;

public record ErrorResonseDto(Integer code, String message, ZonedDateTime timestamp) {}
