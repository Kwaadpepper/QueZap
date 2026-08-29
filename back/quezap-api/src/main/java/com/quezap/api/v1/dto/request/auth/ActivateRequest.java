package com.quezap.api.v1.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record ActivateRequest(@NotBlank(message = "Token cannot be blank") String token) {}
