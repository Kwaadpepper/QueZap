package com.quezap.api.v1.dto.events;

public record SessionGroupMetricsDto(
    int totalParticipants, int averageScore, double globalSuccessRate) {}
