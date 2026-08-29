package com.quezap.api.v1.dto.request.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 3, max = 65, message = "Name must be between 3 and 65 characters") String name,
    @NotBlank(message = "Identifier cannot be blank") String identifier,
    @NotBlank(message = "Password cannot be blank") String password) {}
