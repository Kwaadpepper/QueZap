package com.quezap.api.v1.dto.request.sessions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinSessionRequest(
    @NotBlank(message = "Session code is required") @Pattern(
            regexp = "^[A-Z0-9]{6}$",
            message = "Session code must be 6 alphanumeric characters")
        String code,
    @NotBlank(message = "Participant name is required") @Size(min = 2, max = 50, message = "Participant name must be between 2 and 50 characters") String participantName) {}
