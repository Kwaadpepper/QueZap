package com.quezap.domain.models.valueobjects;

public record SessionCode(String value) {
  public SessionCode {
    if (value.isBlank()) {
      throw new IllegalArgumentException("Session code cannot be blank");
    }
    if (value.trim().length() != 4) {
      throw new IllegalArgumentException("Session code must be 4 characters long");
    }
    if (!value.matches("^[A-Z0-9]{4}$")) {
      throw new IllegalArgumentException("Session code must be alphanumeric and uppercase");
    }
  }
}
