package com.quezap.api.v1.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record ForgottenPasswordRequest(
    @NotBlank(message = "Identifier cannot be blank") String identifier) {}
