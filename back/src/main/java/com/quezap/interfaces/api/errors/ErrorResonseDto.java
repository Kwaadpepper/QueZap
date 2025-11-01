package com.quezap.interfaces.api.errors;

import java.time.ZonedDateTime;

public record ErrorResonseDto(Integer code, String message, ZonedDateTime timestamp) {}
