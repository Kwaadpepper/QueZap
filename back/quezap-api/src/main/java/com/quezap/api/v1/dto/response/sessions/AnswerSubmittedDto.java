package com.quezap.api.v1.dto.response.sessions;

import java.time.Instant;

public record AnswerSubmittedDto(Boolean accepted, Instant receivedAt) {}
