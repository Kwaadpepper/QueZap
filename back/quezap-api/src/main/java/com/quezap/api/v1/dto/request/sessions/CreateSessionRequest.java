package com.quezap.api.v1.dto.request.sessions;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateSessionRequest(
    @NotBlank(message = MSG_REQUIRED) @Size(max = 100, message = MSG_MAX_LENGTH) String name,
    @Nullable UUID quezapId) {
  private static final String MSG_REQUIRED = "Session name is required";
  private static final String MSG_MAX_LENGTH = "Session name cannot exceed 100 characters";
}
