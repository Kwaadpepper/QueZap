package com.quezap.api.v1.dto.request.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "Name cannot be blank") @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters") String name,
    @NotBlank(message = "Email/Identifier cannot be blank") @Email(message = "Invalid email format") String identifier,
    @NotBlank(message = "Password cannot be blank") @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters") String password) {}
