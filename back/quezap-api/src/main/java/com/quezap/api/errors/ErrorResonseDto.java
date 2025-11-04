package com.quezap.api.errors;

import java.time.ZonedDateTime;

public record ErrorResonseDto(Integer code, String message, ZonedDateTime timestamp) {}
