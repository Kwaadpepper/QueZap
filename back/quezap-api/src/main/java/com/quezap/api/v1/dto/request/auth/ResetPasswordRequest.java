package com.quezap.api.v1.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank(message = "Token cannot be blank") String token,
    @NotBlank(message = "New password cannot be blank") String newPassword) {}
