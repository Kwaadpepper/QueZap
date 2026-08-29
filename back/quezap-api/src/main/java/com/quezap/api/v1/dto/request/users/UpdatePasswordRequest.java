package com.quezap.api.v1.dto.request.users;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(
    @NotBlank(message = "Old password cannot be blank") String oldPassword,
    @NotBlank(message = "New password cannot be blank") String newPassword) {}
