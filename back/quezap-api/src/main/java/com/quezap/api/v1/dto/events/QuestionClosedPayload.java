package com.quezap.api.v1.dto.events;

public record QuestionClosedPayload(
    int slideIndex, int totalAnswered, int totalParticipants, String reason) {}
