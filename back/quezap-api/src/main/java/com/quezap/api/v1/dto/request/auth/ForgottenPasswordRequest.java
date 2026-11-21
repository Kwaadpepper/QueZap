package com.quezap.api.v1.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record ForgottenPasswordRequest(
    @NotBlank(message = "Identifier cannot be blank") String identifier,
    @Nullable String turnstileToken) {

  public ForgottenPasswordRequest(String identifier) {
    this(identifier, null);
  }
}
