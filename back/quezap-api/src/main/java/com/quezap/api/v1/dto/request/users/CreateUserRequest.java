package com.quezap.api.v1.dto.request.users;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record CreateUserRequest(
    @Nullable String name,
    @Nullable String identifier,
    @Nullable String email,
    @NotBlank(message = "Password is required") String password,
    @Nullable String turnstileToken) {

  public CreateUserRequest(String name, String identifier, String password) {
    this(name, identifier, identifier, password, null);
  }

  public CreateUserRequest(
      @Nullable String name, @Nullable String identifier, @Nullable String email, String password) {
    this(name, identifier, email, password, null);
  }

  public String getEffectiveName() {
    if (name != null && !name.isBlank()) {
      return name;
    }
    final var id = getEffectiveIdentifier();
    final var atIndex = id.indexOf('@');
    if (atIndex > 0) {
      return id.substring(0, atIndex);
    }
    return "User";
  }

  public String getEffectiveIdentifier() {
    if (identifier != null && !identifier.isBlank()) {
      return identifier;
    }
    if (email != null && !email.isBlank()) {
      return email;
    }
    return "";
  }

  public String getEffectivePassword() {
    if (password != null && !password.isBlank()) {
      return password;
    }
    return "";
  }
}
