package com.quezap.api.v1.dto.request.sessions;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateSessionRequest(
    @NotBlank(message = "Session name cannot be blank") @Size(max = 100, message = "Session name cannot exceed 100 characters") String name,
    @Nullable UUID quezapId) {}
