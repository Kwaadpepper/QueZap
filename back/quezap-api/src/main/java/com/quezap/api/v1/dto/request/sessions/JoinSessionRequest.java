package com.quezap.api.v1.dto.request.sessions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinSessionRequest(
    @NotBlank(message = "Code cannot be blank") String code,
    @NotBlank(message = "Participant name cannot be blank") @Size(min = 3, max = 65, message = "Participant name must be between 3 and 65 characters") String participantName) {}
