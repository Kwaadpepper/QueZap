package com.quezap.api.v1.dto.events;

import java.time.Instant;

public record ParticipantDto(String name, Instant joinedAt) {}
