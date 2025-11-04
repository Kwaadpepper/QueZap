package com.quezap.lib.ddd.exceptions;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

public class IllegalDomainStateException extends IllegalStateException {
  public IllegalDomainStateException(String message) {
    super(message);
  }

  public IllegalDomainStateException(String message, Throwable cause) {
    super(message, cause);
  }

  public IllegalDomainStateException(Throwable cause) {
    super(cause);
  }

  public IllegalDomainStateException() {
    super();
  }

  @Override
  public @NonNull String getMessage() {
    return Objects.requireNonNull(super.getMessage());
  }

  public static Runnable throwWith(String message) {
    return () -> {
      throw new IllegalDomainStateException(message);
    };
  }
}
