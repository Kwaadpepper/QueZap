package com.quezap.application.exceptions;

import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;

public class ApplicationConstraintException extends RuntimeException {
  private final ApplicationErrorCode error;

  public ApplicationConstraintException(ApplicationErrorCode error) {
    super(error.getMessage());
    this.error = error;
  }

  public ApplicationConstraintException(ApplicationErrorCode error, String message) {
    super(message);
    this.error = error;
  }

  public ApplicationConstraintException(ApplicationErrorCode error, Throwable cause) {
    super(error.getMessage(), cause);
    this.error = error;
  }

  public Integer getCode() {
    return error.getCode();
  }

  @Override
  public @NonNull String getMessage() {
    return Objects.requireNonNull(super.getMessage());
  }

  public static Runnable throwWith(ApplicationErrorCode error) {
    return () -> {
      throw new ApplicationConstraintException(error);
    };
  }

  public static Supplier<ApplicationConstraintException> with(ApplicationErrorCode error) {
    return () -> new ApplicationConstraintException(error);
  }
}
