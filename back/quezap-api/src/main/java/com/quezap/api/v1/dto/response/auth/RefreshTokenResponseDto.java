package com.quezap.api.v1.dto.response.auth;

public record RefreshTokenResponseDto(
    String accessToken, String refreshToken, String tokenType, long expiresIn) {}
