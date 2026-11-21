package com.quezap.api.v1.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyResetTokenRequest(@NotBlank(message = "Token is required") String token) {}
