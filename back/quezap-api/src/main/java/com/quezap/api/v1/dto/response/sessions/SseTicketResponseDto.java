package com.quezap.api.v1.dto.response.sessions;

public record SseTicketResponseDto(String ticket, long expiresInSeconds) {}
