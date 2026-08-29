package com.quezap.api.v1.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token cannot be blank") String refreshToken) {}
